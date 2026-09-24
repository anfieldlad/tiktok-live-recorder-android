# Device spike: youtubedl-android without `--impersonate`

Throwaway branch `spike/ytdlp` off `claude/android-backend-dependencies-18926e` (HEAD
`8a012cc`). Only this notes file is kept; the branch itself is never merged.

## Device

Android emulator `emulator-5554`, AVD name `ttl_test`, **x86_64**, abilist
`x86_64,arm64-v8a`, Android 14 (API 34). Not a physical phone. youtubedl-android
ships an x86_64 native build, which is why this emulator (not an arm64 one) was
used for the spike.

## Library version

`io.github.junkfood02.youtubedl-android:library` and `:ffmpeg`, version **0.18.1**
(newest on Maven Central as of 2026-09-24; confirmed via
`maven-metadata.xml` — `<release>0.18.1</release>`, superseding the `0.17.4` the
task brief assumed).

## yt-dlp version

- Bundled/initial (before any update call): **2025.11.12**
- After calling `updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)`:
  **2026.08.19**, `UpdateStatus` returned **`DONE`**

Note: `YoutubeDL.getInstance().versionName(context)` returns `null` the very
first time it's called before `init()`/any execute has produced a version
file — the `reportsVersion` test logged `yt-dlp null` on a fresh app install.
Not a bug, just worth knowing for later tasks that call it eagerly.

Each `connectedDebugAndroidTest` run reinstalls the app fresh, so app-private
storage (and any previous `updateYoutubeDL` call) does not persist between
separate test invocations — every run started back at yt-dlp 2025.11.12.

## Test results

Command used (per-test, since a full run reinstalls the app each time):

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.stillhere.app.spike.YtDlpSpikeTest \
  "-Pandroid.testInstrumentationRunnerArguments.tiktokUrl=<url>" \
  "-Pandroid.testInstrumentationRunnerArguments.instagramUrl=<url>" \
  "-Pandroid.testInstrumentationRunnerArguments.youtubeUrl=<url>"
```

| Test | URL(s) tried | Result | Time |
|---|---|---|---|
| `reportsVersion` | — | PASS (logged `yt-dlp null` pre-init, informational only) | instant |
| `tiktokVideoWithoutImpersonate` | `tiktok.com/@hankgreen1/video/7047596209028074758` | **FAIL** | ~3.3s to error |
| `tiktokVideoWithoutImpersonate` | `tiktok.com/@pokemonlife22/video/7059698374567611694` (2nd URL, same code path) | **FAIL**, same root cause | ~4.5s to error |
| `instagramReel` | `instagram.com/reels/Cop84x6u7CP/` | FAIL (expected/acceptable, not the gate) | ~10.7s to error |
| `youtubeShortMergesToMp4` | `youtube.com/shorts/BGQWPY4IigY` | **PASS** — downloaded `MET THE LOVE OF MY LIFE ON AN AIRPLANE ＊caught on camera＊-BGQWPY4IigY.mp4` (2,064,274 bytes) | 20,332 ms |
| `updateApiWorks` | — | PASS — `DONE`, 2025.11.12 → 2026.08.19 | ~8.5s |

### TikTok stderr (both URLs — identical failure mode)

```
WARNING: [TikTok] The extractor is attempting impersonation, but no impersonate
target is available. If you encounter errors, then see
https://github.com/yt-dlp/yt-dlp#impersonation for information on installing
the required dependencies
ERROR: [TikTok] <video_id>: Unable to extract webpage video data; please report
this issue on https://github.com/yt-dlp/yt-dlp/issues?q= , filling out the
appropriate issue template. Confirm you are on the latest version using
yt-dlp -U
```

Two different, current, public TikTok videos both failed the same way, on the
bundled yt-dlp 2025.11.12: the TikTok extractor's webpage-data path now
requires an impersonate target (e.g. curl_cffi) to get past TikTok's
anti-bot check, and youtubedl-android has no impersonate target available on
Android. This is a systemic failure, not a dead-link issue.

### Instagram stderr (informational — not the gate)

```
WARNING: [Instagram] Cop84x6u7CP: General metadata extraction failed (some
metadata might be missing).
WARNING: [Instagram] unable to extract shared data; ...
WARNING: [Instagram] Main webpage is locked behind the login page. Retrying
with embed webpage (some metadata might be missing).
WARNING: [Instagram] Unable to download webpage: HTTP Error 404: Not Found
WARNING: [Instagram] unable to extract additional data; ...
ERROR: [Instagram] Cop84x6u7CP: Requested content is not available, rate-limit
reached or login required. Use --cookies-from-browser or --cookies for the
authentication.
```

This matches the expected "login required / rate-limited without cookies"
outcome called out in the task brief. Not a blocker signal by itself.

### YouTube Short — full success path

Ran with `-f "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b" --merge-output-format mp4`
against the bundled ffmpeg. Logged one non-fatal warning
(`YouTube is forcing SABR streaming for this client`, some web-client
https formats skipped) but still produced a playable merged mp4. So YouTube
Shorts download + ffmpeg merge works on-device without impersonation.

## Confirmed API names (for Tasks 10 and 13)

Inspected `library-0.18.1.aar`'s `classes.jar` with `javap` (from
`~/.gradle/caches/modules-2/files-2.1/io.github.junkfood02.youtubedl-android/library/0.18.1/.../library-0.18.1.aar`)
since the brief's exact names needed verifying:

- `com.yausername.youtubedl_android.YoutubeDL.UpdateChannel` — a sealed-style
  Kotlin class exposed to Java as static fields on the outer `UpdateChannel`
  class: **`_STABLE`**, `_NIGHTLY`, `_MASTER` (types
  `UpdateChannel.STABLE` / `.NIGHTLY` / `.MASTER`). The brief's
  `YoutubeDL.UpdateChannel._STABLE` **compiles as written** — no change
  needed.
- `com.yausername.youtubedl_android.YoutubeDL.UpdateStatus` — a plain Java-visible
  enum with exactly two values: **`DONE`**, **`ALREADY_UP_TO_DATE`**.
- `YoutubeDL.execute(...)` overloads (all `public final`, all can throw
  `YoutubeDLException`, `InterruptedException`, `YoutubeDL.CanceledException`):
  - `execute(YoutubeDLRequest)`
  - `execute(YoutubeDLRequest, processId: String)`
  - `execute(YoutubeDLRequest, processId: String, ignoreErrors: Boolean)`
  - `execute(YoutubeDLRequest, processId: String?, callback: (Float, Long, String) -> Unit)`
  - `execute(YoutubeDLRequest, processId: String?, ignoreErrors: Boolean, callback: (Float, Long, String) -> Unit)`
  - The brief's no-arg-except-request call, `execute(request)`, is the plain
    single-argument overload and works as written.
- `YoutubeDLException` lives in **`com.yausername.youtubedl_android`** (top-level
  class, not nested), extends `java.lang.Exception`, with `(String)`,
  `(String, Throwable)`, and `(Throwable)` constructors.
- `init(Context)` and `updateYoutubeDL(Context, UpdateChannel)` are both
  `synchronized` and throw `YoutubeDLException`.
- `versionName(Context)` returns `String` (nullable in practice — see above).
- `FFmpeg.getInstance().init(context)` — unchanged from the brief, no
  compile issues encountered.

No deviation from the brief's method names was needed; the only surprise was
confirming `_STABLE` (with the leading underscore) is really the field name,
not a placeholder in the brief.

## APK size

`app/build/outputs/apk/debug/app-debug.apk`: **218,681,765 bytes (~208.6 MiB)**.
No ABI splits configured on this spike branch, so this single debug APK bundles
native libs for all 4 ABIs (`packaging { jniLibs { useLegacyPackaging = true } }`
was required for `armeabi-v7a`/others to package at all, per the brief).

Per-ABI native lib payload inside that one APK (`unzip -l ... | grep lib/`):

| ABI | `libffmpeg.zip.so` | `libpython.zip.so` | other `.so` (ffmpeg/ffprobe/qjs/graphics/datastore) | ABI subtotal |
|---|---|---|---|---|
| arm64-v8a | 35,624,931 | 14,305,904 | 1,488,388 | **51,419,315 (~49.0 MiB)** |
| armeabi-v7a | 30,476,956 | 12,822,487 | 1,051,116 | **44,350,559 (~42.3 MiB)** |
| x86 | 34,544,829 | 13,979,299 | 1,497,664 | **50,021,792 (~47.7 MiB)** |
| x86_64 (tested device) | 38,595,503 | 14,302,766 | 1,569,248 | **54,467,517 (~51.9 MiB)** |

Sum of all four ABI payloads ≈ 200,259,183 bytes (~191 MiB); the remaining
~17.6 MiB of the APK is dex/resources/assets shared across ABIs. That means an
ABI-split APK would land at roughly:

- arm64-v8a (most real phones): ~51.4 MB native + ~17.6 MB shared ≈ **~69 MB**
- armeabi-v7a: ~44.4 MB + ~17.6 MB ≈ **~62 MB**
- x86_64: ~54.5 MB + ~17.6 MB ≈ **~72 MB**
- x86: ~50.0 MB + ~17.6 MB ≈ **~68 MB**

`libffmpeg.zip.so` and `libpython.zip.so` (the bundled ffmpeg build and the
Python interpreter yt-dlp runs on) dominate the size in every ABI.

## Gate result: BLOCKED

`tiktokVideoWithoutImpersonate` failed against two different, current, public
TikTok videos, both with the same root cause: TikTok's extractor now requires
an impersonate target to get past its anti-bot page, and youtubedl-android has
none available on Android. YouTube Shorts and the update API both work
cleanly without impersonation; Instagram fails in the acceptable
login/rate-limit way described in the brief.

**Per the task's gate, this must go back to the user before any further task
proceeds** — the spec's decision "run yt-dlp without `--impersonate`" needs
revisiting for TikTok specifically. Options worth putting to the user:
bundle an impersonate target if one exists for Android/Chaquopy, accept that
TikTok downloads won't work in the backend-free `local` flavor, or keep TikTok
routed through the existing server backend even in the `local` flavor.

## Files touched on the spike branch (not carried over)

- `gradle/libs.versions.toml` — added `youtubedlAndroid = "0.18.1"` version and
  the `youtubedl-android-library` / `youtubedl-android-ffmpeg` library aliases.
- `app/build.gradle.kts` — added `packaging { jniLibs { useLegacyPackaging = true } }`
  and the two new `implementation(...)` dependencies.
- `app/src/androidTest/java/com/stillhere/app/spike/YtDlpSpikeTest.kt` — new,
  the throwaway instrumented test from the brief (used verbatim).
- `local.properties` — created (gitignored) to point Gradle at the Android SDK
  in this worktree; not part of the spike's git history in any meaningful way.
