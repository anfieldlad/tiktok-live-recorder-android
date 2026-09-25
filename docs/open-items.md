# Open items — Still Here mobile

Everything left before 1.0 can ship, in the order it should be picked up.
Written 2026-08-16.

**State of play:** branch `feat/still-here-mobile`, 10 commits ahead of `main`,
**pushed to origin**. PR not yet opened:
https://github.com/anfieldlad/tiktok-live-recorder-android/pull/new/feat/still-here-mobile
The app builds, 30 unit tests pass, and the instrumented tests compile — but
nothing has run on a real device.

**Two flavors since 2026-09-24.** `server` is this app and is unchanged in
behaviour, so the server-side shim removal it gates still stands. `local` is
new — see `docs/superpowers/specs/2026-09-24-local-flavor-design.md`. Its
device smoke test is item 2.4 below.

Companion doc: `../../tiktok-live-recorder-app/docs/open-items.md`. **This app
shipping is the trigger for deleting two server-side shims**, so closing these
items unblocks work over there.

---

## 1. Before release

### 1.1 Bundle the three fonts

**The single visible gap between what shipped and the design.** `LedgerType`
resolves to the platform's serif and monospace today. Every size, weight and
letter-spacing is already the web's, so proportion and rhythm are correct — the
letterforms are not.

Full instructions in `docs/fonts.md`. It is a four-line change in
`ui/ledger/Type.kt` once the files are in `res/font/`, plus subsetting (Latin
only; Fraunces is variable and only `opsz`, `wght`, `SOFT` are used). Roughly
300–500 KB of APK. All three are OFL.

**When this lands, every screenshot reference will diff** — that is expected, not
a regression. Re-record them:

```bash
./gradlew :app:updateDebugScreenshotTest
```

### 1.2 Replace the README screenshots

`docs/screenshots/*.png` are all of the previous dark-themed app: violet→pink
gradients, rounded cards, neon platform badges. None of it exists any more. The
README already carries a warning banner saying so; delete the banner when the
images are replaced.

Take them after the fonts land, or they will need doing twice.

---

## 2. Verification that needs a device

**No emulator, system image or AVD is installed on this machine**, and no device
is attached — `adb devices` is empty. The UI was reviewed by rendering Compose
previews through Layoutlib on the JVM, which is real rendering but not a real
device: no touch, no system bars, no font scaling, no lifecycle.

To install an emulator:

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"
```

### 2.1 Run the instrumented tests

```bash
./gradlew :app:connectedDebugAndroidTest
```

`LedgerPrimitivesTest` covers all eight primitives. It compiles on every build
via `assembleDebugAndroidTest`, but has never executed.

### 2.2 Smoke-test the four flows that only exist on a device

These are the reason a native app earns its place over the web page, and every
one of them is invisible in a preview:

1. **Share sheet** — share a link from TikTok and from Instagram into the app.
2. **Clipboard banner** — copy a link, background the app, return, confirm the
   offer appears.
3. **MediaStore** — files land in `Movies/StillHere` and `Pictures/StillHere`
   and appear in the gallery. Note the folder changed with the rename; anything
   saved by the old app stays in `TTLDownloader`.
4. **Background download** — start one, leave the app, confirm it completes and
   the notification is right.

### 2.3 Check font scaling

Everything is `sp`, but the nav strip is three labels across a phone. Worth one
look at the largest accessibility font size to confirm nothing clips.

### 2.4 Smoke-test Still Here Local

Install the Local APK for the device's ABI (`app-local-arm64-v8a-debug.apk` on most phones,
`app-local-x86_64-debug.apk` on the emulator) beside the server app and check:
TikTok video · TikTok photo post · Instagram reel · Instagram carousel (signed
in) · Instagram photo post signed out (expect "Sign in to Instagram…") ·
YouTube Short · a long YouTube video (tests the audio/video merge) ·
Sessions → Update now.

The carousel check needs an Instagram sign-in done first, in-app under Sessions — the
person running the test does that themselves.

**Results (2026-09-25, x86_64 emulator, `emulator-5554`, Android 14):**

- Install both apps — **pass**. Two icons installed side by side: "Still Here"
  (`com.stillhere.app`) and "Still Here Local" (`com.stillhere.app.local`).
- Server app unchanged — **pass**. Three tabs present (Record Live,
  Auto-Record, Save Post); Sessions shows the Server section (Backend URL, API
  key, TikTok/Instagram session rows). Saved
  `https://www.tiktok.com/@hankgreen1/video/7047596209028074758` through the
  configured backend: notification "Saved to your gallery" / "1 file(s)",
  file landed in `Movies/StillHere`.
- Local app shell — **pass**. No nav strip on the home screen; Sessions shows
  the Engine section with "yt-dlp yt-dlp 2026.08.19".
- 1. TikTok video — **pass**. URL
  `https://www.tiktok.com/@hankgreen1/video/7047596209028074758`. Notification
  "Saved to your gallery" / "1 file(s)"; `7047596209028074758.mp4` (5.1 MB) in
  `Movies/StillHere`.
- 2. TikTok photo post — **pass**. URL
  `https://www.tiktok.com/@celinedion/photo/7603075722235694344`. Notification
  "Saved to your gallery" / "4 file(s)"; `7603075722235694344-image-001.jpg`
  through `-004.jpg` in `Pictures/StillHere`, in order.
- 3. Instagram reel, signed out — **pass**. URL
  `https://www.instagram.com/reel/DcBx_I1iHZW/` (NASA). Notification "Saved to
  your gallery" / "1 file(s)"; `Video by nasa-DcBx_I1iHZW.mp4` (13.6 MB) in
  `Movies/StillHere`.
- 4. Instagram photo post, signed out — **pass**. URL
  `https://www.instagram.com/p/DbGvdS8IDTu/` (NASA). Notification exactly as
  expected: "Download failed" / "Sign in to Instagram to save photo posts."
- 5. Instagram carousel, signed in — **needs a human sign-in — not run.**
- 6. YouTube Short — **pass**. URL
  `https://www.youtube.com/shorts/BGQWPY4IigY`. Notification "Saved to your
  gallery" / "1 file(s)"; file in `Movies/StillHere`.
- 7. YouTube video over 5 minutes — **pass**. URL
  `https://www.youtube.com/watch?v=UF8uR6Z6KLc` ("Steve Jobs' 2005 Stanford
  Commencement Address", ~15 min). Notification "Saved to your gallery" / "1
  file(s)"; single MP4 (21.5 MB) in `Movies/StillHere`, confirmed to contain
  both a `vide` and a `soun` handler atom (audio/video merged, not
  video-only).
- 8. Sessions → Update now — **pass**. Tapping it changed the Engine caption
  from "Updated." to "Already the newest.", version number "2026.08.19" shown
  throughout.
- 9. Leave the app while saving — **pass**. Shared
  `https://www.tiktok.com/@pokemonlife22/video/7059698374567611694`, then
  immediately pressed Home; the notification still completed to "Saved to
  your gallery" / "1 file(s)" with the app backgrounded, and
  `7059698374567611694.mp4` landed in `Movies/StillHere`.
- Cleanup — **pass**. `adb shell run-as com.stillhere.app.local ls cache/jobs`
  returned empty (no leftover job directories).
- No crashes observed in `logcat` (`AndroidRuntime`/`FATAL EXCEPTION`) across
  the run.

---

## 3. Deliberate trade-offs — not bugs

| Decision | Consequence |
|---|---|
| New `applicationId` (`com.stillhere.app`) | Android sees a **different app**. The old one stays installed as a separate icon and must be removed by hand. Backend URL, API key and local history do **not** carry over. A settings migration was offered and declined. |
| Light theme only | No dark mode, matching the web. One palette to design, build and test. |
| No animated margin rule | The web pulses the rule on in-flight cards. Here `live` widens it 3dp→5dp instead — a pulsing rule in a phone list costs more than it says. |
| Compose UI tests are instrumented | `createComposeRule` needs a device. The logic worth asserting without one — stamp vocabulary, tokens, contrast, relative time, wire models — is covered by JVM tests that run on every build. |

---

## 4. Design system notes

- **Contrast is now enforced by a test.** `ContrastTest` asserts every text
  colour against both surfaces plus all three stamp inks. It caught `Pending` at
  4.15:1 on its first run. A token nudged "a shade lighter" for looks will fail
  the build.
- **Screenshot references are committed** under
  `app/src/debug/screenshotTest/reference/`. `./gradlew
  :app:validateDebugScreenshotTest` diffs against them.
- **The web is the source of truth for wording.** Any copy change should be made
  on both sides. `stampFor()` is the only place that decides what a status is
  called — keep it that way.

---

## 5. Out of scope by decision

From the mobile spec: dark mode, batch multi-URL paste, a home-screen widget,
iOS, and any change to the web app. Each is independent and additive.
