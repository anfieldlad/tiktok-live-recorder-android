# Still Here Local — design

> **Status:** implemented, on branch `claude/android-backend-dependencies-18926e`.
> Written 2026-09-24. The device spike below ran on an x86_64 emulator, not a
> physical phone.
>
> **This is phase 1 of two.** Phase 2 (live recording on the phone) is unchanged
> from the sketch below — it gets its own spec once its own device spike has
> passed.

**Goal:** a second build of the app that works with no backend at all. It saves
TikTok, Instagram and YouTube posts to the gallery by running the extraction on the
phone. The existing app stays exactly as it is.

## Decisions

Taken during brainstorming, with the reasoning that produced them:

1. **Two apps, one codebase, as product flavors.** `server` is today's app;
   `local` is new. One UI, so a Ledger fix lands in both. Rejected: a separate
   `:core` module (bigger refactor up front for a boundary flavors already give
   us) and a fork (the two would drift).
2. **The local flavor drops auto-record and the shared register.** A phone cannot
   watch a creator around the clock reliably — Doze, the 15-minute WorkManager floor
   and the `dataSync` foreground-service time cap all work against it. The register
   is server state by definition. History stays device-local, as it already is.
3. **Engine: yt-dlp on the phone for Instagram and YouTube; TikTok read natively.** Via
   `youtubedl-android`, which bundles Python, yt-dlp and ffmpeg and can update yt-dlp
   in place. Same extractor as the server, so the same Instagram and YouTube links
   work. TikTok is different: the device spike found yt-dlp cannot read TikTok on
   Android at all, because its TikTok extractor now requires an impersonation
   target (a browser-realistic TLS/HTTP fingerprint via `curl_cffi`) that
   `youtubedl-android` has no way to provide. A follow-up spike found a plain OkHttp
   client — riding Android's own TLS stack — reads TikTok's page and media without
   any impersonation at all, so TikTok gets its own reader, `TikTokEngine`, and never
   goes through yt-dlp. See `docs/superpowers/plans/2026-09-24-local-flavor-spike.md`
   for both spikes. Rejected: pure Kotlin extractors for everything (we would own
   every breakage yt-dlp's maintainers absorb today, YouTube especially) and Chaquopy
   (no ffmpeg, and updates need a new APK).
4. **Two phases.** Phase 1 ships saving and on-device sessions. Live recording is
   the least certain part — room lookup, an unimpersonated `webcast` call, and a
   running FLV→fMP4 remux — so it waits for its own spike and does not hold phase 1
   hostage.
5. **tikwm stays, as a fallback only.** "No backend" means no *own* server. tikwm is
   a third party that sees the link, kept for whatever `TikTokEngine` cannot read —
   the native reader is tried first for every TikTok link (video, photo post, short
   link alike), and tikwm only runs when it fails.

## Flavors

One dimension, `engine`:

| | `server` | `local` |
|---|---|---|
| `applicationId` | `com.stillhere.app` (unchanged — installs keep upgrading) | `com.stillhere.app.local` |
| App name | Still Here | Still Here Local |
| Icon | as today | the same mark on a different background ink |
| ABI splits | none | `arm64-v8a`, `armeabi-v7a`, `x86_64` |

Both install side by side. `x86_64` is for the emulator, not phones. The local APK
carries native Python and ffmpeg, so each ABI's APK lands around 66–76 MB against
the server APK's ~18 MB — acceptable for a sideloaded app. Splits cannot be set per
flavor in Gradle, and the server flavor has no native code to split, so splitting
switches on only when the Gradle invocation names a Local task (`assembleLocalDebug`,
`assembleLocalRelease`); build each flavor with its own command, never a bare
`assemble`/`build`.

## Seams

Today shared code calls `ApiClient` directly in four places: `DownloadManager`,
`LiveRecordingService`, `SessionLoginActivity` and `AppViewModel`. Shared code will
instead depend on two interfaces in `src/main`:

```kotlin
/** One media file on its way to the gallery. [index] is 1-based. */
data class IncomingFile(val index: Int, val total: Int, val name: String, val contentType: String?)

/** Turns a routed link into media files. Failures are thrown as [MediaException]
 *  with a message written for the person who shared the link. */
interface MediaSource {
    suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit)
}

/** Where a platform's login cookie lives. */
interface SessionStore {
    /** Whether sessions can be managed right now. The server flavor needs a backend URL first; local is always. */
    suspend fun isAvailable(): Boolean
    suspend fun isConnected(platform: Platform): Boolean
    suspend fun save(platform: Platform, sessionId: String)
    suspend fun clear(platform: Platform)
}
```

Each flavor provides a `FlavorModule` with the same fully-qualified name, which
builds its `MediaSource`, its `SessionStore` and a `Features` value:

```kotlin
data class Features(val destinations: List<Destination>, val sessionsNote: String)
```

Server settings and Local's yt-dlp "Engine" section are flavor composables
(`FlavorSettingsSection`), not flags, and each flavor supplies its own
`FlavorDestination` composable for the tabs `Features.destinations` lists.
`StillHereApp` asks `FlavorModule` for these instead of constructing `ApiClient`.

### Where code lives

- **`src/main`** — the Ledger UI, `SavePostScreen`, `SessionsSheet`,
  `SessionLoginActivity`, `DownloadManager` (still owns progress, `saver.save`, the
  history write and `Done`; it now calls `MediaSource`), `MediaStoreSaver`,
  `HistoryRepo`, `UrlRouter`, the share and clipboard entry points, the two
  interfaces and `Features`.
- **`src/server`** — `ApiClient` and its models, `ServerMediaSource` (submit → poll
  → `withFile`, today's `DownloadManager` body), `ServerSessionStore`, backend URL
  and API key settings, and everything for auto-record. `AutoRecordScreen` moves
  here and its state leaves `AppViewModel` for a new `WatchViewModel`. Live
  recording (`LiveRecordingService`, `RecordLiveScreen`, the live state in
  `AppViewModel`) also moves here for phase 1.
- **`src/local`** — `LocalMediaSource`, the yt-dlp wrapper, the native `TikTokEngine`,
  the tikwm and Instagram fallbacks, `LocalSessionStore`, and the Engine section in
  Settings.

**The rule:** anything that touches the backend lives only in `src/server`. If
`src/local` compiles, the local app has no backend in it.

### Navigation

`AppRoot` builds its tabs from `Features`. The server flavor keeps Record live /
Auto-record / Save post. The local flavor in phase 1 has only Save post, so the
bottom nav bar is not drawn. The masthead and the Sessions sheet are unchanged,
except that the local Sessions sheet has no backend URL or API key fields.

## The local save pipeline

`LocalMediaSource.fetch` runs inside the existing foreground `DownloadService`, one
download at a time as `DownloadController` already enforces.

### 1. Prepare

Create `cacheDir/jobs/<uuid>/`. If the platform has a stored session, write a
Netscape `cookies.txt` holding `sessionid` for the platform's domain into it. The
rest of the pipeline runs in `try/finally`; the `finally` deletes the whole
directory, cookie file included.

### 2. Route

| Link | First | On failure or no media |
|---|---|---|
| TikTok video, photo post or short link | native reader (`TikTokEngine`) | tikwm |
| Instagram reel / video | yt-dlp | Instagram extractor |
| Instagram photo / carousel | yt-dlp | Instagram extractor |
| YouTube video / Short | yt-dlp | none — error |

yt-dlp fails fast on Instagram photo posts, so trying it first costs little and
keeps one path for mixed carousels that it can handle. TikTok never reaches
yt-dlp at all — see decision 3 and the spike notes for why — so every TikTok
link, video, photo post or short link alike, goes to the native reader first.

### 3. Extract

**`TikTokEngine`** reads TikTok with plain OkHttp and a cookie jar scoped to the
one save (the signed-in session's `sessionid`, when there is one). It fetches the
page — OkHttp follows a short link's redirects itself, so a video, a photo post
and a `vm.tiktok.com`/`vt.tiktok.com` short link all start the same way — and reads
the post from whichever of two places TikTok used for that response: the
rehydration script's (`__UNIVERSAL_DATA_FOR_REHYDRATION__`) `…-detail` entry, or,
on a cookie-less first request, the separate `api-data` script. It then fetches the
chosen media URL(s) with that same cookie jar: the CDN answers HTTP 403 without the
cookies the page just set, for video and (untested but assumed) photo alike.

**yt-dlp** gets the server's arguments minus `--impersonate` (no `curl_cffi` on
Android): `--no-playlist`, `--check-formats`, `--add-header "Referer: <platform
origin>/"`, `--paths <job dir>`, `--output "%(title).80s-%(id)s.%(ext)s"`, and
`--cookies <file>` when a session exists. YouTube also gets
`-f "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b" --merge-output-format mp4`, so the
gallery always gets an MP4; the bundled ffmpeg does the merge.

**tikwm** ports `_download_with_metadata_fallback` from the server's
`post_download_service.py`: one `GET https://www.tikwm.com/api/?url=…`, then fetch
each of `data.images` or, failing that, `data.play`, falling back to the
watermarked `data.wmplay` (the server prefers `wmplay`; Local prefers the clean
copy). Relative paths are resolved against `https://www.tikwm.com`. It keeps the
server's guards: media URLs must resolve to public addresses — the same guard the
native reader and the Instagram extractor use, `PublicUrl`, which also refuses
IETF protocol-assignment, TEST-NET and benchmarking IPv4 ranges and IPv6
discard-only, documentation and IETF ranges, and unwraps 6to4 and NAT64
(`64:ff9b::/96`) addresses to classify the IPv4 address embedded inside rather
than reject the whole range — NAT64 is how an IPv6-only mobile network reaches an
IPv4 host, so a public address reached through it must still read as public. Each
file is streamed with a size ceiling. No `metadata.json` is written.

**The Instagram extractor** does what gallery-dl does. Decode the post's shortcode to
its numeric media id, call `GET https://www.instagram.com/api/v1/media/{id}/info/`
with the `sessionid` cookie and the web app-id header, then take the largest
`image_versions2` candidate or `video_versions` entry for each item (each
`carousel_media` entry, or the post itself). It needs a session. Without one it
fails with *"Sign in to Instagram to save photo posts."*

### 4. Hand off

Every file in the job directory that `MediaFiles.isMedia` accepts goes to `onFile`
in filename order (carousel order for the fallbacks); it skips `.json`, `.txt`,
`.nfo`, `.part` and `.ytdl` sidecars and leftovers. The filter lives in `src/main`
and both flavors share it. `DownloadManager` saves each through `MediaStoreSaver`,
then writes history and emits `Done`.

### Sessions

`LocalSessionStore` keeps `sessionid` per platform in a separate `sessions`
Preferences DataStore in app-private storage. It is never sent anywhere except the
platform it belongs to. `clear` also removes that platform's cookies from the WebView
`CookieManager`, so the next sign-in shows a fresh login page.
`SessionLoginActivity` calls `sessionStore.save(...)` instead of
`api.saveSession(...)`.

## yt-dlp lifecycle

- **Init.** `YoutubeDL.init` and `FFmpeg.init` run on first use, off the main thread.
  The first run unpacks Python and takes seconds; the existing `Resolving` state
  covers it.
- **Updates.** On app start, a background check against the stable channel, at most
  once per 24 hours and never while a download runs. Settings shows
  *"yt-dlp <version> · Update now"*.
- **Nudge.** An extractor-shaped failure ends with *"Updating yt-dlp in Settings
  often fixes this."*

## Errors

One `FriendlyError.from(stderr)`, checked in this order — login is checked before
rate-limit and gone, because yt-dlp's login walls often also mention rate limits or
unavailability:

| Signal | Message |
|---|---|
| no network | No connection. Check your network and try again. |
| private / login required | Sign in to {platform} to save this. (YouTube gets its own line: it won't serve this without a login, which Still Here doesn't keep.) |
| HTTP 429 / rate limited | {platform} is rate-limiting. Try again in a few minutes. |
| removed / unavailable | This post is gone — deleted, private, or region-locked. |
| unsupported URL | That link isn't a post Still Here can save. |
| a broken extractor | Only yt-dlp's issue-tracker link (`github.com/yt-dlp/yt-dlp/issues`) marks this, not its FAQ link — an error asking the reader to open an issue is aimed at a yt-dlp maintainer, not at someone who pasted a link. {platform} did not return this post, plus the update-yt-dlp nudge. |
| anything else | last meaningful stderr line |

When a fallback also fails, the **first** engine's error is shown — it is usually
the one that explains the post. Exception: yt-dlp's "There is no video in this
post" never counts as the first error — on a photo post it only means yt-dlp was
the wrong tool, and the next engine's error explains the post.

## Testing

**JVM, every build:**
- route selection per the table above
- cookie-file writer (Netscape format, correct domain)
- Instagram shortcode → media id
- parsing Instagram `media/info` and tikwm responses from recorded JSON fixtures
  (single image, single video, mixed carousel)
- `friendlyError` against real yt-dlp stderr samples
- each flavor's `Features` (`testServer` / `testLocal` source sets)

**Build:** `assembleServerDebug` and `assembleLocalDebug` both compile, and the
existing tests pass for both flavors. The compile is what enforces the rule that
`src/local` never sees `ApiClient`. Screenshot tests keep running against the server
flavor.

**Device smoke (manual):** TikTok video, TikTok photo post, Instagram reel,
Instagram carousel signed in, Instagram photo post signed out (clear error), YouTube
Short, a long YouTube video (exercises the merge), Update now.

## Spike — the first task of the plan

On a real device, before any refactor:

1. `youtubedl-android` builds against AGP 8.9 / compileSdk 36 / minSdk 26.
2. A TikTok video downloads with the arguments above, without `--impersonate`.
3. An Instagram reel and a YouTube Short download.

If step 2 fails, stop and revisit decision 3 before building the rest.

## Risks

- **No device on this machine.** `docs/open-items.md` records no emulator, no system
  image and nothing attached. The spike cannot run without one; neither can the
  smoke test. This blocks phase 1, not just its verification.
- **No impersonation.** The server always passes `--impersonate chrome`. A phone on
  a residential or mobile IP is treated more leniently than a server, but TikTok may
  still refuse plain requests. The spike answers this.
- **Instagram's private API shape.** `media/info` is what gallery-dl relies on and
  it changes. It is isolated in one small class with fixture tests so a break is
  a one-file fix.
- **Bundled yt-dlp ages.** Mitigated by in-app updates; an old APK on a fresh
  install still starts on an old yt-dlp until the first update check.

## Phase 2 — sketch only

Bring Record live back to the local flavor behind a `LiveSource` seam: read `roomId`
from `tiktok.com/@user/live` in a hidden WebView; call `webcast/room/info` over
OkHttp with the local `sessionid`; pick the FLV by the server's quality order
(`extract_live_url` in `live_stream.py`); pipe it through the bundled ffmpeg with
`-c copy -f mp4 -movflags frag_keyframe+empty_moov` into MediaStore, reusing
`LiveRecordingService`'s structure. Specified separately after its own spike.

## Docs

- README gains a "Two flavors" section: what each does, how to build each
  (`assembleServerRelease`, `assembleLocalRelease`), and that Local needs no setup.
- `docs/open-items.md` notes the server flavor is unchanged, so the server-side shim
  removal it gates still stands.

## Out of scope

Auto-record and the register in the local flavor (by definition); live recording
(phase 2); download percentage while yt-dlp runs; parallel downloads; quality
choice; moving data between the two apps; any server change.
