# Still Here mobile — design

> **Status:** designed, not implemented. Written 2026-08-14 to be executed in a
> later session. Generate the task-by-task plan from this with
> `superpowers:writing-plans` before touching code.
>
> **Depends on two server specs shipping first**, both in the
> `tiktok-live-recorder-app` repo under `docs/superpowers/specs/`:
> `2026-08-14-concurrent-download-jobs-design.md` and
> `2026-08-14-api-key-session-gating-design.md`.

**Goal:** rebuild the Android app as *Still Here* — visually and verbally selaras with
the redesigned web app — while keeping the things a native app can do that a web page
cannot.

## Where the two products are today

The web app was redesigned as **the Ledger**: a light paper register. Cream card stock
on a ruled board, an oxblood margin rule, entries numbered, statuses applied as rubber
stamps, three serif faces (Fraunces, Newsreader, Cutive Mono), zero corner radius.

The Android app is its visual opposite: Jetpack Compose with Material3, a **dark**
theme, a violet→pink `BrandGradient`, per-platform neon gradients (TikTok cyan/red,
Instagram sunset), rounded corners and a `HeroGlow`. Screens are Home (composer,
progress, history), Live, and Settings.

So this is not a re-skin. It is a rewrite of the UI layer.

### What the app has that the web does not

These are the reason a native app earns its place, and none may be lost in the rewrite:

- **Share-sheet receiver** — share a link from TikTok or Instagram straight into the app.
- **MediaStore saving** — files land in the phone's gallery, not a server directory.
- **Clipboard detection** — offers the URL already copied.
- **Local history** — what this device has saved.
- **Background service with notifications** — downloads survive leaving the app.

### What the web has that the app does not

- **Auto-record.** No equivalent screen exists on Android. See
  [Auto-record](#auto-record-is-the-new-feature).

## Decisions

Taken during brainstorming, with the reasoning that produced them:

1. **Shared design language, native idioms.** Same palette, serifs, stamps, register
   cards and voice, so it is unmistakably the same product — but a bottom sheet rather
   than a side drawer, real touch targets, back gesture, share sheet. Selaras in feel
   without fighting the platform.
2. **Light only.** No dark theme, matching the web. Paper is paper. One palette to
   design, build and test, and no risk of a half-finished second theme.
3. **Mirror the web's three sections**, so both products share one mental model and no
   feature is phone-only or web-only.
4. **Server specs ship first**; the app is built against the finished API.
5. **New application id**, accepting a fresh install. See [Naming](#naming).

## Design language

A `LedgerTheme` replaces the Material3 dark theme, carrying the web's token layer:
board and card surfaces, ink and dim text, rule hairlines, the oxblood series ink and
the second Instagram ink, and the three stamp colours (filed / pending / failed).
Corner radius is zero throughout; rules are 1.5px.

Alongside it, a `ui/ledger/` package of primitives mirroring the web's CSS vocabulary:

| Primitive | Web equivalent |
|---|---|
| `Sheet` | `.sheet` — the entry form card with its oxblood margin rule |
| `EntryCard` | `.job-card` — a filed entry, with margin rule and register number |
| `Stamp` | `.stamp` — rotated, bordered status mark |
| `LedgerField` | `.field` — mono label above an underlined input |
| `LedgerButton` | `.btn` / `.btn-quiet` — square, mono, letter-spaced |
| `Eyebrow` | `.eyebrow` — mono uppercase kicker |
| `FiledHead` | `.filed-head` — section label with a trailing rule |

**Building from a shared vocabulary, rather than pixel-matching screens, is what keeps
the two selaras as they drift.** When the web adds a component, this table gains a row.

### Fonts

Fraunces, Newsreader and Cutive Mono are **bundled as assets**, not fetched through the
Google Fonts provider: no Play Services dependency, works offline, and no flash of
fallback type on a cold start. Cost is roughly 300–500 KB of APK. All three are OFL
licensed, so redistribution is clear.

Subset aggressively — the app needs Latin only, and Fraunces is a variable font whose
full axis range is not used. Only `opsz`, `wght` and `SOFT` are referenced by the web
design.

## Structure

Bottom navigation with three destinations, matching the web's tab strip:

- **Record live** — start a capture now.
- **Auto-record** — a standing order for a creator who is offline.
- **Save post** — paste a TikTok or Instagram link.

Sessions move into a **modal bottom sheet** with the same two sections (TikTok,
Instagram) the web drawer has. The backend URL and API key live there too, rather than
in a separate Settings screen — one place for "how this app talks to my server".

The share-sheet receiver, clipboard banner, MediaStore saving, history and background
service are all **kept and restyled**, not dropped.

## Code structure

`AppUi.kt` is 947 lines holding every screen. The redesign rewrites essentially all of
it, so this is the moment to split it:

```
ui/
  ledger/        Theme.kt, Tokens.kt, Type.kt, primitives (one file per primitive)
  record/        RecordLiveScreen.kt
  watch/         AutoRecordScreen.kt
  save/          SavePostScreen.kt
  sessions/      SessionsSheet.kt
  AppRoot.kt     navigation only
```

This is a targeted improvement justified by the work at hand, not speculative
refactoring: the files being split are the files being rewritten.

## Auto-record is the new feature

It does not exist on Android and is the single largest item in this project — the most
likely thing to slip, and the one to schedule first rather than last.

It needs: request/response models for the watch endpoints, `ApiClient` methods against
the existing `/watch-recordings` routes (which already work and are unchanged by the
server specs), a screen following the same Sheet + Filed-list shape as the other two,
and list rendering with `Watching` / `Recording` / `Completed` / `Failed` stamps.

## API adoption

- **Base URL** default moves to `/stillhere`. Launching on it is what lets the server
  delete the `/tiktok` nginx shim.
- **Downloads** use the async job API: submit, receive an id, poll. Launching on it is
  what lets the server delete the synchronous download door.
- **`X-API-Key`** is already implemented in `ApiClient.applyApiKey()` and needs no
  client change — only a server that enforces it, and the key entered in the app.

Nothing is retired on the client. This app shipping is the trigger for retiring **two
server-side shims**, and that should be stated in its release notes so the server work
actually gets done.

## Naming

| Level | Now | Becomes |
|---|---|---|
| `applicationId` | `com.ttldownloader.app` | `com.stillhere.app` |
| `namespace` | `com.ttldownloader.app` | `com.stillhere.app` |
| `app_name` | `TTL Downloader` | `Still Here` |
| `versionCode` / `versionName` | 5 / 1.4 | 1 / 1.0 — a new identity starts a new line |

**Accepted trade-off, chosen deliberately after the consequences were shown:** changing
`applicationId` makes this a *different app* to Android. The existing install remains
alongside it as a separate icon and must be removed by hand; the backend URL, API key
and local history do not carry over and must be re-entered. A settings migration was
offered and declined. If the app is ever published, this is a new listing with no
install history.

## Copy

The rule: **register voice inside the app, plain language on system surfaces.**

In-app text carries the web's vocabulary. Notifications, the share-sheet target label
and permission rationales stay plain, because they appear out of context beside other
apps, where "Filing your entry" reads as a malfunction rather than a voice.

### In-app — shared with the web, verbatim where possible

| Surface | Copy |
|---|---|
| Record live eyebrow / headline / action | `Entry — live capture · TikTok` / "Record a broadcast before it's gone" / `Begin capture` |
| Auto-record | `Entry — standing order · TikTok` / "Wait for a broadcast that hasn't started" / `Place the order` |
| Save post | `Entry — saved post` / "Save a post before it's gone" / `Save post` |
| Field labels | `Subject — username or live URL`, `Link — TikTok or Instagram`, `Duration in seconds — optional` |
| List heading | `Filed` |
| Empty state | "Nothing filed yet" |
| Stamps | `Queued` · `Working` · `Filed` · `Failed` · `Ready` · `Pending` |
| Sessions | `Sessions`, `TikTok session`, `Instagram session` |

Any copy change on either side should be made on both. The web is the source of truth
for wording.

### System surfaces — deliberately plain

| Surface | Copy |
|---|---|
| Launcher / share target | `Still Here` |
| Notification channel | `Downloads` — "Shows progress while media is saving" |
| Working notification | "Saving from TikTok" / "Saving from Instagram" |
| Finished notification | "Saved to your gallery" |
| Failure notification | The server's error message, unchanged |

The app's own tagline, *"A register of things published once"*, belongs on the home
screen and any about screen — not in a notification.

## Testing

- Compose UI tests for each `ui/ledger/` primitive, since every screen depends on them.
- **The share-sheet and clipboard flows must survive the rewrite.** They are
  regression-prone, invisible in normal use until someone tries them, and they are the
  app's real advantage over the web. Test them explicitly.
- MediaStore saving still lands files in the gallery.
- A download continues when the app is backgrounded.
- Screens compared against the web at equivalent widths, checking the shared vocabulary
  renders consistently rather than checking pixels.

## Risks

1. **Auto-record is genuinely new work**, not a port. Schedule it first.
2. **This is a UI-layer rewrite**, so the estimate should reflect that rather than
   "restyle".
3. **Fresh install** loses configuration — see [Naming](#naming).
4. **Font size and licensing** are settled, but subsetting needs doing rather than
   assuming.

## Out of scope

Dark mode, batch multi-URL paste, a home-screen widget, iOS, and any change to the
web app. Each is independent and additive.
