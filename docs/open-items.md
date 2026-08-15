# Open items — Still Here mobile

Everything left before 1.0 can ship, in the order it should be picked up.
Written 2026-08-16.

**State of play:** branch `feat/still-here-mobile`, 9 commits ahead of `main`,
**not pushed**. The app builds, 30 unit tests pass, and the instrumented tests
compile — but nothing has run on a real device.

Companion doc: `../../tiktok-live-recorder-app/docs/open-items.md`. **This app
shipping is the trigger for deleting two server-side shims**, so closing these
items unblocks work over there.

---

## 1. Cannot push from here

The remote is HTTPS with no cached credentials:

```
fatal: could not read Username for 'https://github.com': terminal prompts disabled
```

The server repo uses SSH (`git@github.com:anfieldlad/…`) and pushes fine. Either
push by hand, or switch this repo to match:

```bash
git remote set-url origin git@github.com:anfieldlad/tiktok-live-recorder-android.git
```

---

## 2. Before release

### 2.1 Bundle the three fonts

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

### 2.2 Replace the README screenshots

`docs/screenshots/*.png` are all of the previous dark-themed app: violet→pink
gradients, rounded cards, neon platform badges. None of it exists any more. The
README already carries a warning banner saying so; delete the banner when the
images are replaced.

Take them after the fonts land, or they will need doing twice.

---

## 3. Verification that needs a device

**No emulator, system image or AVD is installed on this machine**, and no device
is attached — `adb devices` is empty. The UI was reviewed by rendering Compose
previews through Layoutlib on the JVM, which is real rendering but not a real
device: no touch, no system bars, no font scaling, no lifecycle.

To install an emulator:

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"
```

### 3.1 Run the instrumented tests

```bash
./gradlew :app:connectedDebugAndroidTest
```

`LedgerPrimitivesTest` covers all eight primitives. It compiles on every build
via `assembleDebugAndroidTest`, but has never executed.

### 3.2 Smoke-test the four flows that only exist on a device

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

### 3.3 Check font scaling

Everything is `sp`, but the nav strip is three labels across a phone. Worth one
look at the largest accessibility font size to confirm nothing clips.

---

## 4. Deliberate trade-offs — not bugs

| Decision | Consequence |
|---|---|
| New `applicationId` (`com.stillhere.app`) | Android sees a **different app**. The old one stays installed as a separate icon and must be removed by hand. Backend URL, API key and local history do **not** carry over. A settings migration was offered and declined. |
| Light theme only | No dark mode, matching the web. One palette to design, build and test. |
| No animated margin rule | The web pulses the rule on in-flight cards. Here `live` widens it 3dp→5dp instead — a pulsing rule in a phone list costs more than it says. |
| Compose UI tests are instrumented | `createComposeRule` needs a device. The logic worth asserting without one — stamp vocabulary, tokens, contrast, relative time, wire models — is covered by JVM tests that run on every build. |

---

## 5. Design system notes

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

## 6. Out of scope by decision

From the mobile spec: dark mode, batch multi-URL paste, a home-screen widget,
iOS, and any change to the web app. Each is independent and additive.
