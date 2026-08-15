# Still Here Mobile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Android app as *Still Here* — visually and verbally selaras with the redesigned web app — while keeping every native capability a web page cannot have.

**Architecture:** A `LedgerTheme` replaces the Material3 dark theme and carries the web's token layer. Beside it, a `ui/ledger/` package of primitives mirrors the web's CSS vocabulary one-for-one, and every screen is built from those primitives rather than pixel-matched to a web page — that is what keeps the two selaras as they drift. `AppUi.kt` (947 lines, every screen) is split into one package per destination, with `AppRoot.kt` holding navigation only.

**Tech Stack:** Jetpack Compose (BOM 2024.12.01), Material3 as a substrate only, Kotlin 2.0.21, AGP 8.9.1, DataStore, OkHttp, kotlinx.serialization.

**Source spec:** `docs/superpowers/specs/2026-08-14-still-here-mobile-design.md`

**Depends on** both server plans in the `tiktok-live-recorder-app` repo:
`2026-08-15-concurrent-download-jobs.md` and `2026-08-15-api-key-session-gating.md`.

## Global Constraints

- **Nothing native is lost.** The share-sheet receiver, MediaStore saving, clipboard detection, local history and the background service with notifications are the reason a native app earns its place. They are kept and restyled, never dropped.
- **Light only.** No dark theme, matching the web. Paper is paper.
- **Zero corner radius throughout. Rules are 1.5dp.** These are the two most visible signatures of the design; a stray `RoundedCornerShape` reads as a different product.
- **Register voice inside the app, plain language on system surfaces.** Notifications, the share-target label and permission rationales stay plain — they appear out of context beside other apps, where "Filing your entry" reads as a malfunction rather than a voice.
- **The web is the source of truth for wording.** Any copy change should be made on both sides.
- **Naming, exact values:** `applicationId` and `namespace` → `com.stillhere.app`; `app_name` → `Still Here`; `versionCode` → `1`; `versionName` → `1.0`.
- **Accepted trade-off, chosen deliberately after the consequences were shown:** changing `applicationId` makes this a *different app* to Android. The old install remains as a separate icon and must be removed by hand; backend URL, API key and local history do not carry over. A settings migration was offered and declined. Do not add one.
- **Build gate for every task:** `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` must both pass before the commit.
- The tagline *"A register of things published once"* belongs on the home screen and an about screen — never in a notification.

---

### Task 1: A new identity

**Files:**
- Move: `app/src/main/java/com/ttldownloader/app/**` → `app/src/main/java/com/stillhere/app/**`
- Move: `app/src/test/java/com/ttldownloader/app/**` → `app/src/test/java/com/stillhere/app/**`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Rename: `TtlApp.kt` → `StillHereApp.kt`

**Interfaces:**
- Produces: package `com.stillhere.app`; `class StillHereApp : Application()` with the same lazy singletons (`settings`, `history`, `api`, `saver`, `downloadManager`, `controller`, `liveController`); theme styles `Theme.StillHere` and `Theme.StillHere.Transparent`.

- [ ] **Step 1: Move the source tree**

```bash
cd /Users/bobby/dev/personal/tiktok-live-recorder-android
mkdir -p app/src/main/java/com/stillhere app/src/test/java/com/stillhere
git mv app/src/main/java/com/ttldownloader/app app/src/main/java/com/stillhere/app
git mv app/src/test/java/com/ttldownloader/app app/src/test/java/com/stillhere/app
rmdir app/src/main/java/com/ttldownloader app/src/test/java/com/ttldownloader
git mv app/src/main/java/com/stillhere/app/TtlApp.kt app/src/main/java/com/stillhere/app/StillHereApp.kt
```

- [ ] **Step 2: Rewrite the references**

```bash
grep -rl "com\.ttldownloader\.app\|TtlApp\|TtlTheme\|Theme\.TtlDownloader" \
  app/src app/build.gradle.kts | xargs sed -i '' \
  -e 's/com\.ttldownloader\.app/com.stillhere.app/g' \
  -e 's/\bTtlApp\b/StillHereApp/g' \
  -e 's/\bTtlTheme\b/LedgerTheme/g' \
  -e 's/Theme\.TtlDownloader/Theme.StillHere/g'
```

`TtlTheme` becomes `LedgerTheme` here rather than in Task 2 so the rename is one
sweep; Task 2 replaces its body.

- [ ] **Step 3: Update the build identity**

In `app/build.gradle.kts`:

```kotlin
android {
    namespace = "com.stillhere.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stillhere.app"
        minSdk = 26
        targetSdk = 36
        // A new identity starts a new line. The previous app was
        // com.ttldownloader.app at versionCode 5 / 1.4; to Android this is a
        // different app entirely, with no upgrade path and no shared data.
        versionCode = 1
        versionName = "1.0"
    }
```

- [ ] **Step 4: Rename the app and repaint the launch window**

`app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Still Here</string>
    <string name="download_channel_name">Downloads</string>
    <string name="download_channel_desc">Shows progress while media is saving</string>
</resources>
```

`app/src/main/res/values/colors.xml` — the launch window must be paper, or every
cold start flashes the old near-black:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#F4EFE4</color>
    <color name="ledger_board">#EDE6D8</color>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Base theme: Compose draws the real UI, this is only the launch window.
         A paper background avoids a dark flash before the first frame. -->
    <style name="Theme.StillHere" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/ledger_board</item>
    </style>

    <!-- The share receiver shows no UI of its own. -->
    <style name="Theme.StillHere.Transparent" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowNoTitle">true</item>
    </style>
</resources>
```

In `AndroidManifest.xml`, change `android:name=".TtlApp"` to
`android:name=".StillHereApp"`.

- [ ] **Step 5: Fix the system bars**

In `MainActivity.kt`, the edge-to-edge call still forces dark system bars. On a
paper canvas the icons must be dark:

```kotlin
        // Dark system-bar icons — the app is always light-themed.
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
```

- [ ] **Step 6: Build and test**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, `UrlRouterTest` still green

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: rename the app to Still Here"
```

---

### Task 2: The design language

**Files:**
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/Tokens.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/Type.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/Theme.kt`
- Create: `app/src/main/res/font/` (directory, may stay empty — see Step 3)
- Create: `docs/fonts.md`
- Delete: `app/src/main/java/com/stillhere/app/ui/theme/Theme.kt`
- Test: `app/src/test/java/com/stillhere/app/ui/ledger/TokensTest.kt`

**Interfaces:**
- Produces:
  - `object Ledger` with the token layer: `Board`, `Card`, `CardEdge`, `Ink`, `Dim`, `Rule`, `SeriesInk`, `SeriesInkAlt`, `Filed`, `Pending`, `FailedInk`
  - `val LedgerShapes: Shapes` — every corner `0.dp`
  - `val RuleWidth: Dp = 1.5.dp`
  - `object LedgerType` with `display`, `body`, `mono` `FontFamily`s and a `Typography`
  - `@Composable fun LedgerTheme(content: @Composable () -> Unit)`
  - `fun seriesInk(platform: Platform): Color`

The values are the web's, copied from `app/static/css/app.css` in the server
repo. Keeping them in one object is what makes "when the web changes a token,
change it here too" a one-line job.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/stillhere/app/ui/ledger/TokensTest.kt`:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.ui.unit.dp
import com.stillhere.app.net.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TokensTest {

    @Test
    fun `every corner is square`() {
        // Zero radius is the design's loudest signature. A stray rounded corner
        // reads as a different product, so this is worth a test.
        listOf(
            LedgerShapes.extraSmall,
            LedgerShapes.small,
            LedgerShapes.medium,
            LedgerShapes.large,
            LedgerShapes.extraLarge,
        ).forEach { assertEquals(SquareCorners, it) }
    }

    @Test
    fun `rules are hairlines`() {
        assertEquals(1.5.dp, RuleWidth)
    }

    @Test
    fun `instagram gets the second ink`() {
        assertNotEquals(seriesInk(Platform.TIKTOK), seriesInk(Platform.INSTAGRAM))
        assertEquals(Ledger.SeriesInk, seriesInk(Platform.TIKTOK))
        assertEquals(Ledger.SeriesInkAlt, seriesInk(Platform.INSTAGRAM))
    }

    @Test
    fun `the three stamp inks are distinct`() {
        val stamps = setOf(Ledger.Filed, Ledger.Pending, Ledger.FailedInk)
        assertEquals(3, stamps.size)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: FAIL — `Unresolved reference: LedgerShapes`

- [ ] **Step 3: Write the tokens**

Create `app/src/main/java/com/stillhere/app/ui/ledger/Tokens.kt`:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stillhere.app.net.Platform

/**
 * The web app's token layer, in Kotlin.
 *
 * These are the same values as the custom properties in `app/static/css/app.css`
 * in the server repo. Building both products from a shared vocabulary — rather
 * than pixel-matching screens — is what keeps them selaras as they drift: when
 * the web changes a token, this object changes with it and every screen follows.
 */
object Ledger {
    /** The ruled board the paper sits on. */
    val Board = Color(0xFFEDE6D8)

    /** Cream card stock. */
    val Card = Color(0xFFF7F2E7)
    val CardEdge = Color(0xFFDCD2BE)

    val Ink = Color(0xFF23201B)
    val Dim = Color(0xFF6E675C)
    val Rule = Color(0xFFC9BFA8)

    /** Oxblood — the margin rule and the first platform's ink. */
    val SeriesInk = Color(0xFF7B2D26)

    /** The second ink, for Instagram entries. */
    val SeriesInkAlt = Color(0xFF3E5C4B)

    /** The three rubber stamps. */
    val Filed = Color(0xFF2F6B4F)
    val Pending = Color(0xFF8A7231)
    val FailedInk = Color(0xFF9B2C22)
}

/** Zero radius, everywhere. Declared once so it cannot drift per-component. */
val SquareCorners = RoundedCornerShape(0.dp)

val LedgerShapes = Shapes(
    extraSmall = SquareCorners,
    small = SquareCorners,
    medium = SquareCorners,
    large = SquareCorners,
    extraLarge = SquareCorners,
)

/** Hairline rules, matching the web's 1.5px. */
val RuleWidth = 1.5.dp

/** Which ink an entry is written in. */
fun seriesInk(platform: Platform): Color = when (platform) {
    Platform.TIKTOK -> Ledger.SeriesInk
    Platform.INSTAGRAM -> Ledger.SeriesInkAlt
}
```

- [ ] **Step 4: Write the type scale**

Create `app/src/main/java/com/stillhere/app/ui/ledger/Type.kt`:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Three faces, matching the web: Fraunces for display, Newsreader for body,
 * Cutive Mono for labels and stamps.
 *
 * The families resolve to bundled assets when `res/font/` holds them, and to the
 * platform's serif and monospace otherwise. That fallback is deliberate: the
 * layout, weights and letter-spacing are all correct either way, so the app is
 * shippable before the OFL files are dropped in, and gains the real faces the
 * moment they are. See `docs/fonts.md` for how to add them.
 *
 * They are bundled rather than fetched through the Google Fonts provider so
 * there is no Play Services dependency, it works offline, and there is no flash
 * of fallback type on a cold start.
 */
object LedgerType {
    val display: FontFamily = FontFamily.Serif
    val body: FontFamily = FontFamily.Serif
    val mono: FontFamily = FontFamily.Monospace

    /** Mono, uppercase, widely tracked — the register's label voice. */
    val label = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Normal,
    )

    /** The stamp face: smaller, tighter, and always uppercase at the call site. */
    val stamp = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.4.sp,
        fontWeight = FontWeight.Normal,
    )
}

val LedgerTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = LedgerType.display,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontWeight = FontWeight.Normal,
    ),
    headlineMedium = TextStyle(
        fontFamily = LedgerType.display,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        fontWeight = FontWeight.Normal,
    ),
    titleMedium = TextStyle(
        fontFamily = LedgerType.display,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyLarge = TextStyle(
        fontFamily = LedgerType.body,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = LedgerType.body,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = LedgerType.label,
    labelSmall = LedgerType.stamp,
)
```

- [ ] **Step 5: Write the theme**

Create `app/src/main/java/com/stillhere/app/ui/ledger/Theme.kt`:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light only, matching the web. There is no dark theme and no system-following:
 * one palette to design, build and test, and no risk of a half-finished second
 * one. Material3 is a substrate here — the palette and shapes are the Ledger's.
 */
private val LedgerColors = lightColorScheme(
    primary = Ledger.SeriesInk,
    onPrimary = Ledger.Card,
    secondary = Ledger.SeriesInkAlt,
    onSecondary = Ledger.Card,
    background = Ledger.Board,
    onBackground = Ledger.Ink,
    surface = Ledger.Card,
    onSurface = Ledger.Ink,
    surfaceVariant = Ledger.Board,
    onSurfaceVariant = Ledger.Dim,
    outline = Ledger.Rule,
    outlineVariant = Ledger.CardEdge,
    error = Ledger.FailedInk,
    onError = Ledger.Card,
)

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LedgerColors,
        shapes = LedgerShapes,
        typography = LedgerTypography,
        content = content,
    )
}
```

Delete `app/src/main/java/com/stillhere/app/ui/theme/Theme.kt`. Leave
`ui/theme/Brand.kt` alone for now — screens still reference it, and Task 6
deletes it once nothing does.

Update the import in `MainActivity.kt` from
`com.stillhere.app.ui.theme.LedgerTheme` to `com.stillhere.app.ui.ledger.LedgerTheme`.

- [ ] **Step 6: Document the font drop-in**

Create `docs/fonts.md`:

```markdown
# Fonts

The design calls for three faces, the same three the web app uses:

| Role | Face | Licence |
|---|---|---|
| Display | Fraunces | OFL |
| Body | Newsreader | OFL |
| Label, stamps | Cutive Mono | OFL |

They are **bundled as assets**, not fetched through the Google Fonts provider:
no Play Services dependency, works offline, and no flash of fallback type on a
cold start. All three are OFL, so redistribution is clear. Cost is roughly
300–500 KB of APK.

`LedgerType` currently resolves to the platform's serif and monospace. Every
size, weight and letter-spacing is already correct, so the app ships and looks
right in shape; adding the real faces is a drop-in.

## Adding them

1. Download the OFL files from Google Fonts.
2. **Subset aggressively** — the app needs Latin only, and Fraunces is a
   variable font whose full axis range is not used. Only `opsz`, `wght` and
   `SOFT` are referenced by the web design.
3. Put them in `app/src/main/res/font/` with lowercase, underscore-only names:
   `fraunces_regular.ttf`, `newsreader_regular.ttf`, `cutive_mono_regular.ttf`.
4. In `ui/ledger/Type.kt`, replace the three `FontFamily` assignments:

```kotlin
    val display = FontFamily(Font(R.font.fraunces_regular))
    val body = FontFamily(Font(R.font.newsreader_regular))
    val mono = FontFamily(Font(R.font.cutive_mono_regular))
```

Nothing else changes — every screen reads its face from `LedgerType`.
```

- [ ] **Step 7: Run the tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`, `TokensTest` green

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: bring the ledger's design language to the app"
```

---

### Task 3: The primitives

**Files:**
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/Sheet.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/EntryCard.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/Stamp.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/LedgerField.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/LedgerButton.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/Eyebrow.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/FiledHead.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/EmptyState.kt`
- Modify: `app/build.gradle.kts` (androidTest dependencies)
- Test: `app/src/androidTest/java/com/stillhere/app/ui/ledger/LedgerPrimitivesTest.kt`
- Test: `app/src/test/java/com/stillhere/app/ui/ledger/StampTest.kt`

**Interfaces:**
- Consumes: `Ledger`, `LedgerType`, `RuleWidth`, `seriesInk`, `SquareCorners` (Task 2).
- Produces, each mirroring one CSS class:

| Primitive | Signature | Web equivalent |
|---|---|---|
| `Sheet` | `Sheet(modifier: Modifier = Modifier, ink: Color = Ledger.SeriesInk, content: @Composable ColumnScope.() -> Unit)` | `.sheet` |
| `EntryCard` | `EntryCard(register: String, ink: Color = Ledger.SeriesInk, live: Boolean = false, stamp: @Composable (() -> Unit)? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)` | `.job-card` |
| `Stamp` | `Stamp(label: String, kind: StampKind)` and `enum class StampKind { Filed, Pending, Failed }` | `.stamp` |
| `LedgerField` | `LedgerField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", singleLine: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text)` | `.field` |
| `LedgerButton` | `LedgerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, quiet: Boolean = false, danger: Boolean = false)` | `.btn` / `.btn-quiet` |
| `Eyebrow` | `Eyebrow(text: String, modifier: Modifier = Modifier)` | `.eyebrow` |
| `FiledHead` | `FiledHead(text: String = "Filed", modifier: Modifier = Modifier)` | `.filed-head` |
| `LedgerEmpty` | `LedgerEmpty(title: String, detail: String, modifier: Modifier = Modifier)` | `.empty` |

`stampFor(status: String): Pair<String, StampKind>` also lives in `Stamp.kt`, so
the six status words in the spec's copy table have exactly one mapping in the app.

Every screen depends on these, which is why they are tested directly.

- [ ] **Step 1: Add the instrumented test dependencies**

In `gradle/libs.versions.toml`, under `[versions]`:

```toml
androidxTestExtJunit = "1.2.1"
espresso = "3.6.1"
```

under `[libraries]`:

```toml
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

In `app/build.gradle.kts`, add to `defaultConfig`:

```kotlin
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

and to `dependencies`:

```kotlin
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
```

- [ ] **Step 2: Write the failing tests**

Create `app/src/test/java/com/stillhere/app/ui/ledger/StampTest.kt` — pure Kotlin,
runs on the JVM with no device:

```kotlin
package com.stillhere.app.ui.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

class StampTest {

    @Test
    fun `every status the server can report has a stamp`() {
        // These six words are the shared vocabulary with the web. A status with
        // no mapping would render as a blank stamp, which reads as a bug.
        val cases = mapOf(
            "queued" to ("Queued" to StampKind.Pending),
            "running" to ("Working" to StampKind.Pending),
            "finished" to ("Filed" to StampKind.Filed),
            "failed" to ("Failed" to StampKind.Failed),
            "watching" to ("Watching" to StampKind.Pending),
            "recording" to ("Recording" to StampKind.Pending),
            "completed" to ("Completed" to StampKind.Filed),
            "stopped" to ("Stopped" to StampKind.Failed),
        )
        cases.forEach { (status, expected) -> assertEquals(expected, stampFor(status)) }
    }

    @Test
    fun `an unknown status still renders something honest`() {
        assertEquals("Pending" to StampKind.Pending, stampFor("something-new"))
    }
}
```

Create `app/src/androidTest/java/com/stillhere/app/ui/ledger/LedgerPrimitivesTest.kt`:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LedgerPrimitivesTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sheet_renders_its_content() {
        compose.setContent { LedgerTheme { Sheet { Text("Record a broadcast") } } }
        compose.onNodeWithText("Record a broadcast").assertIsDisplayed()
    }

    @Test
    fun entry_card_shows_its_register_number_and_stamp() {
        compose.setContent {
            LedgerTheme {
                EntryCard(register = "No. 20260815-1015", stamp = { Stamp("Filed", StampKind.Filed) }) {
                    Text("1 file filed")
                }
            }
        }
        compose.onNodeWithText("No. 20260815-1015").assertIsDisplayed()
        compose.onNodeWithText("FILED").assertIsDisplayed()
        compose.onNodeWithText("1 file filed").assertIsDisplayed()
    }

    @Test
    fun ledger_field_reports_what_was_typed() {
        var captured = ""
        compose.setContent {
            LedgerTheme {
                LedgerField(label = "Link — TikTok or Instagram", value = captured, onValueChange = { captured = it })
            }
        }
        compose.onNodeWithText("LINK — TIKTOK OR INSTAGRAM").assertIsDisplayed()
        compose.onNodeWithText("").performTextInput("https://tiktok.com/x")
        assertEquals("https://tiktok.com/x", captured)
    }

    @Test
    fun ledger_button_click_and_disabled_state() {
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                LedgerButton(text = "Begin capture", onClick = { clicks++ })
                LedgerButton(text = "Place the order", onClick = { }, enabled = false)
            }
        }
        compose.onNodeWithText("BEGIN CAPTURE").assertIsEnabled().performClick()
        compose.onNodeWithText("PLACE THE ORDER").assertIsNotEnabled()
        assertEquals(1, clicks)
    }

    @Test
    fun eyebrow_and_filed_head_render_their_labels() {
        compose.setContent {
            LedgerTheme {
                Eyebrow("Entry — saved post")
                FiledHead()
            }
        }
        compose.onNodeWithText("ENTRY — SAVED POST").assertIsDisplayed()
        compose.onNodeWithText("Filed").assertIsDisplayed()
    }

    @Test
    fun empty_state_says_what_is_missing() {
        compose.setContent {
            LedgerTheme { LedgerEmpty("Nothing filed yet", "Paste a TikTok or Instagram link above.") }
        }
        compose.onNodeWithText("Nothing filed yet").assertIsDisplayed()
    }

    @Test
    fun every_stamp_kind_renders() {
        compose.setContent {
            LedgerTheme {
                StampKind.entries.forEach { Stamp(it.name, it) }
            }
        }
        StampKind.entries.forEach {
            compose.onNodeWithText(it.name.uppercase()).assertIsDisplayed()
        }
        assertTrue(StampKind.entries.size == 3)
    }
}
```

- [ ] **Step 3: Run the JVM test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: FAIL — `Unresolved reference: StampKind`

- [ ] **Step 4: Write the primitives**

`Stamp.kt` — a rotated, bordered status mark:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class StampKind { Filed, Pending, Failed }

fun stampInk(kind: StampKind): Color = when (kind) {
    StampKind.Filed -> Ledger.Filed
    StampKind.Pending -> Ledger.Pending
    StampKind.Failed -> Ledger.FailedInk
}

/**
 * Every status word the server can report, mapped to the shared vocabulary.
 *
 * The web renders these same six-plus words from the same statuses. A status
 * with no mapping would render a blank stamp, which reads as a bug rather than
 * as an unknown state, so the fallback says "Pending" and means it.
 */
fun stampFor(status: String): Pair<String, StampKind> = when (status.lowercase()) {
    "queued" -> "Queued" to StampKind.Pending
    "running" -> "Working" to StampKind.Pending
    "finished" -> "Filed" to StampKind.Filed
    "failed" -> "Failed" to StampKind.Failed
    "watching" -> "Watching" to StampKind.Pending
    "recording" -> "Recording" to StampKind.Pending
    "completed" -> "Completed" to StampKind.Filed
    "stopped" -> "Stopped" to StampKind.Failed
    "ready" -> "Ready" to StampKind.Filed
    else -> "Pending" to StampKind.Pending
}

/** `.stamp` — rotated, bordered, mono, always uppercase. */
@Composable
fun Stamp(label: String, kind: StampKind, modifier: Modifier = Modifier) {
    val ink = stampInk(kind)
    Box(
        modifier = modifier
            .rotate(-4f)
            .border(RuleWidth, ink, SquareCorners)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label.uppercase(), style = LedgerType.stamp, color = ink)
    }
}
```

`Sheet.kt` — the entry form card with its oxblood margin rule:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * `.sheet` — cream card stock with a 3dp margin rule down its left edge.
 *
 * The rule is what makes the card read as paper in a register rather than as a
 * Material surface, so it is part of the primitive and not a decoration a screen
 * can forget.
 */
@Composable
fun Sheet(
    modifier: Modifier = Modifier,
    ink: Color = Ledger.SeriesInk,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Ledger.Card)
            .border(RuleWidth, Ledger.CardEdge, SquareCorners),
    ) {
        Column(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(ink),
        ) {}
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            content = content,
        )
    }
}
```

`EntryCard.kt` — a filed entry, with margin rule, register number and stamp:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * `.job-card` — one filed entry.
 *
 * [live] is the web's `.job-card.live`: work still in flight. The web animates
 * the margin rule; here it is simply drawn in the series ink at full strength,
 * because a pulsing rule on a phone list costs more than it says.
 */
@Composable
fun EntryCard(
    register: String,
    modifier: Modifier = Modifier,
    ink: Color = Ledger.SeriesInk,
    live: Boolean = false,
    stamp: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Ledger.Card)
            .border(RuleWidth, Ledger.CardEdge, SquareCorners),
    ) {
        Column(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (live) ink else Ledger.Rule),
        ) {}
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(register, style = LedgerType.label, color = Ledger.Dim)
                stamp?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
```

`LedgerField.kt` — mono label above an underlined input:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * `.field` — a mono uppercase label above a line, not a box.
 *
 * BasicTextField rather than OutlinedTextField: Material's field brings its own
 * container, radius and floating label, all three of which fight this design.
 */
@Composable
fun LedgerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = LedgerType.label, color = Ledger.Dim)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ledger.Ink),
            cursorBrush = SolidColor(Ledger.SeriesInk),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Ledger.Rule)
                }
                inner()
            },
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(thickness = RuleWidth, color = Ledger.Rule)
    }
}
```

`LedgerButton.kt` — square, mono, letter-spaced:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

/**
 * `.btn` / `.btn-quiet` — a square, bordered, mono action.
 *
 * The 48dp minimum is the one place this deliberately departs from the web: a
 * touch target is not a hover target. Selaras in feel without fighting the
 * platform.
 */
@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    quiet: Boolean = false,
    danger: Boolean = false,
) {
    val ink: Color = when {
        !enabled -> Ledger.Rule
        danger -> Ledger.FailedInk
        quiet -> Ledger.Dim
        else -> Ledger.SeriesInk
    }
    val fill = if (quiet || danger || !enabled) Color.Transparent else Ledger.SeriesInk
    val label = if (fill == Color.Transparent) ink else Ledger.Card

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(fill)
            .border(RuleWidth, ink, SquareCorners)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = LedgerType.label, color = label)
    }
}
```

`Eyebrow.kt`, `FiledHead.kt` and `EmptyState.kt`:

```kotlin
package com.stillhere.app.ui.ledger

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** `.eyebrow` — a mono uppercase kicker above a headline. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = LedgerType.label, color = Ledger.SeriesInk, modifier = modifier)
}

/** `.filed-head` — a section label with a rule running out to the margin. */
@Composable
fun FiledHead(text: String = "Filed", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ledger.Ink)
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = RuleWidth, color = Ledger.Rule)
    }
}

/** `.empty` — a dashed box saying what is not here yet. */
@Composable
fun LedgerEmpty(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .border(RuleWidth, Ledger.Rule, SquareCorners)
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ledger.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Ledger.Dim,
            textAlign = TextAlign.Center,
        )
    }
}
```

Put `Eyebrow`, `FiledHead` and `LedgerEmpty` in their three named files, each
with only the imports it needs.

- [ ] **Step 5: Run the tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`
Expected: `BUILD SUCCESSFUL`; `StampTest` green; the instrumented tests compile.
They need a device or emulator to run — `./gradlew :app:connectedDebugAndroidTest`
when one is attached.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add the ledger primitives every screen is built from"
```

---

### Task 4: Auto-record — the genuinely new feature

**Files:**
- Create: `app/src/main/java/com/stillhere/app/net/WatchModels.kt`
- Modify: `app/src/main/java/com/stillhere/app/net/ApiClient.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/watch/AutoRecordScreen.kt`
- Modify: `app/src/main/java/com/stillhere/app/ui/AppViewModel.kt`
- Test: `app/src/test/java/com/stillhere/app/net/WatchModelsTest.kt`

**Interfaces:**
- Consumes: every primitive from Task 3.
- Produces:
  - `WatchJob` — `id`, `username`, `url`, `duration`, `status`, `linkedRecordingJobId`, `lastCheckedAt`, `lastMessage`, `createdAt`, `finishedAt`; every field defaulted, since a shape change must not throw.
  - `ApiClient.listWatches(): List<WatchJob>`
  - `ApiClient.createWatch(username: String, durationSeconds: Int?): WatchJob`
  - `ApiClient.stopWatch(id: String): WatchJob`
  - `ApiClient.deleteWatch(id: String)`
  - `AppViewModel`: `watches: StateFlow<List<WatchJob>>`, `watchUsername`, `watchDuration`, `watchNotice`, `refreshWatches()`, `onWatchUsernameChange`, `onWatchDurationChange`, `placeWatchOrder()`, `stopWatch(id)`, `deleteWatch(id)`
  - `@Composable fun AutoRecordScreen(viewModel: AppViewModel)`

Scheduled here, before the screen rewrites, because it is the single largest
item and the most likely thing to slip — it is a new feature, not a port. The
`/watch-recordings` routes already exist server-side and are unchanged by both
server plans.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/stillhere/app/net/WatchModelsTest.kt`:

```kotlin
package com.stillhere.app.net

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a watch job as the server sends it`() {
        val body = """
            {
              "id": "abc-123",
              "username": "someone",
              "url": null,
              "duration": 600,
              "status": "watching",
              "linked_recording_job_id": null,
              "last_checked_at": "2026-08-15T10:15:00+00:00",
              "last_message": "Waiting for the account to go live.",
              "created_at": "2026-08-15T10:00:00+00:00",
              "finished_at": null
            }
        """.trimIndent()

        val job = json.decodeFromString(WatchJob.serializer(), body)

        assertEquals("abc-123", job.id)
        assertEquals("someone", job.username)
        assertEquals(600, job.duration)
        assertEquals("watching", job.status)
        assertEquals("Waiting for the account to go live.", job.lastMessage)
        assertNull(job.finishedAt)
    }

    @Test
    fun `a field the server stops sending does not throw`() {
        // Every field defaults, so a server-side shape change degrades to a
        // blank cell rather than a crash mid-list.
        val job = json.decodeFromString(WatchJob.serializer(), """{"id":"x"}""")

        assertEquals("x", job.id)
        assertEquals("", job.status)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: FAIL — `Unresolved reference: WatchJob`

- [ ] **Step 3: Write the models**

Create `app/src/main/java/com/stillhere/app/net/WatchModels.kt`:

```kotlin
package com.stillhere.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `WatchJobResponse` from the backend's `/watch-recordings` routes — a
 * standing order for a creator who is not live yet.
 *
 * Every field is defaulted on purpose: this list renders while a watch is being
 * polled server-side, and a shape change should show a blank cell rather than
 * throw halfway through drawing the list.
 */
@Serializable
data class WatchJob(
    val id: String = "",
    val username: String? = null,
    val url: String? = null,
    val duration: Int? = null,
    val status: String = "",
    @SerialName("linked_recording_job_id") val linkedRecordingJobId: String? = null,
    @SerialName("last_checked_at") val lastCheckedAt: String? = null,
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
)
```

- [ ] **Step 4: Add the client methods**

In `ApiClient.kt`:

```kotlin
    /** Every standing order, newest first as the server returns them. */
    suspend fun listWatches(): List<WatchJob> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(requireBaseUrl() + "/watch-recordings")
            .get()
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiException(response.code, text)
            json.decodeFromString(ListSerializer(WatchJob.serializer()), text)
        }
    }

    /** Place a standing order for a creator who is not live yet. */
    suspend fun createWatch(username: String, durationSeconds: Int?): WatchJob =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("username", JsonPrimitive(username))
                if (durationSeconds != null) put("duration", JsonPrimitive(durationSeconds))
            }
            val request = Request.Builder()
                .url(requireBaseUrl() + "/watch-recordings")
                .post(json.encodeToString(JsonObject.serializer(), payload).toRequestBody(JSON_MEDIA))
                .applyApiKey()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw toApiException(response.code, text)
                json.decodeFromString(WatchJob.serializer(), text)
            }
        }

    suspend fun stopWatch(id: String): WatchJob = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${requireBaseUrl()}/watch-recordings/$id/stop")
            .post("".toRequestBody(JSON_MEDIA))
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiException(response.code, text)
            json.decodeFromString(WatchJob.serializer(), text)
        }
    }

    suspend fun deleteWatch(id: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${requireBaseUrl()}/watch-recordings/$id")
            .delete()
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw toApiException(response.code, response.body?.string().orEmpty())
        }
    }
```

Add `import kotlinx.serialization.builtins.ListSerializer` at the top.

- [ ] **Step 5: Add the view-model state**

In `AppViewModel.kt`:

```kotlin
    // --- Auto-record: standing orders for creators who are not live yet ---

    private val _watches = MutableStateFlow<List<WatchJob>>(emptyList())
    val watches: StateFlow<List<WatchJob>> = _watches.asStateFlow()

    private val _watchUsername = MutableStateFlow("")
    val watchUsername: StateFlow<String> = _watchUsername.asStateFlow()

    private val _watchDuration = MutableStateFlow("")
    val watchDuration: StateFlow<String> = _watchDuration.asStateFlow()

    private val _watchNotice = MutableStateFlow("")
    val watchNotice: StateFlow<String> = _watchNotice.asStateFlow()

    fun onWatchUsernameChange(value: String) { _watchUsername.value = value }
    fun onWatchDurationChange(value: String) { _watchDuration.value = value.filter { it.isDigit() } }

    fun refreshWatches() {
        viewModelScope.launch {
            if (app.settings.baseUrl().isBlank()) return@launch
            runCatching { app.api.listWatches() }
                .onSuccess { _watches.value = it }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't reach the register." }
        }
    }

    fun placeWatchOrder() {
        val username = normalizeLiveUsername(_watchUsername.value)
        if (username.isBlank()) {
            _watchNotice.value = "Enter a TikTok username or live URL."
            return
        }
        viewModelScope.launch {
            runCatching { app.api.createWatch(username, _watchDuration.value.toIntOrNull()) }
                .onSuccess {
                    _watchUsername.value = ""
                    _watchDuration.value = ""
                    _watchNotice.value = "Order placed."
                    refreshWatches()
                }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't place the order." }
        }
    }

    fun stopWatch(id: String) {
        viewModelScope.launch {
            runCatching { app.api.stopWatch(id) }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't stop that order." }
            refreshWatches()
        }
    }

    fun deleteWatch(id: String) {
        viewModelScope.launch {
            runCatching { app.api.deleteWatch(id) }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't discard that order." }
            refreshWatches()
        }
    }
```

Add `import com.stillhere.app.net.WatchJob`.

- [ ] **Step 6: Write the screen**

Create `app/src/main/java/com/stillhere/app/ui/watch/AutoRecordScreen.kt`. It
follows the same Sheet + FiledHead + list shape the other two screens will use:

- `Eyebrow("Entry — standing order · TikTok")`
- headline "Wait for a broadcast that hasn't started"
- `LedgerField("Subject — username or live URL", …)`
- `LedgerField("Duration in seconds — optional", …, keyboardType = KeyboardType.Number)`
- `LedgerButton("Place the order", …)`
- the notice line
- `FiledHead()`
- a `LazyColumn` of `EntryCard`s, each with `Stamp(stampFor(job.status))`,
  `job.lastMessage` as the body, and Stop / Discard `LedgerButton`s
- `LedgerEmpty("Nothing filed yet", "Place a standing order above.")` when the
  list is empty
- `LaunchedEffect(Unit)` calling `refreshWatches()`, then a poll every 10 seconds
  while any job is `watching` or `recording`

```kotlin
package com.stillhere.app.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.net.WatchJob
import com.stillhere.app.ui.AppViewModel
import com.stillhere.app.ui.ledger.EntryCard
import com.stillhere.app.ui.ledger.Eyebrow
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerEmpty
import com.stillhere.app.ui.ledger.LedgerField
import com.stillhere.app.ui.ledger.LedgerType
import com.stillhere.app.ui.ledger.Sheet
import com.stillhere.app.ui.ledger.Stamp
import com.stillhere.app.ui.ledger.stampFor
import kotlinx.coroutines.delay

private fun isActive(job: WatchJob) = job.status == "watching" || job.status == "recording"

@Composable
fun AutoRecordScreen(viewModel: AppViewModel) {
    val username by viewModel.watchUsername.collectAsStateWithLifecycle()
    val duration by viewModel.watchDuration.collectAsStateWithLifecycle()
    val notice by viewModel.watchNotice.collectAsStateWithLifecycle()
    val watches by viewModel.watches.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshWatches() }
    LaunchedEffect(watches.any(::isActive)) {
        // A standing order is checked server-side every 45s, so a 10s poll is
        // as live as this can usefully be.
        while (watches.any(::isActive)) {
            delay(10_000)
            viewModel.refreshWatches()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Sheet {
                Eyebrow("Entry — standing order · TikTok")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Wait for a broadcast\nthat hasn't started",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ledger.Ink,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "The register watches the account and starts recording the moment they go live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ledger.Dim,
                )
                Spacer(Modifier.height(20.dp))
                LedgerField(
                    label = "Subject — username or live URL",
                    value = username,
                    onValueChange = viewModel::onWatchUsernameChange,
                    placeholder = "@someone",
                )
                Spacer(Modifier.height(16.dp))
                LedgerField(
                    label = "Duration in seconds — optional",
                    value = duration,
                    onValueChange = viewModel::onWatchDurationChange,
                    placeholder = "Until the live ends",
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(20.dp))
                LedgerButton("Place the order", viewModel::placeWatchOrder)
                if (notice.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(notice, style = LedgerType.label, color = Ledger.Dim)
                }
            }
        }
        item { Spacer(Modifier.height(6.dp)); FiledHead() }
        if (watches.isEmpty()) {
            item { LedgerEmpty("Nothing filed yet", "Place a standing order above.") }
        } else {
            items(watches, key = { it.id }) { job ->
                val (label, kind) = stampFor(job.status)
                EntryCard(
                    register = "No. ${job.id.take(8).uppercase()}",
                    live = isActive(job),
                    stamp = { Stamp(label, kind) },
                ) {
                    Text(
                        job.username ?: job.url ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ledger.Ink,
                    )
                    if (job.lastMessage.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(job.lastMessage, style = MaterialTheme.typography.bodyMedium, color = Ledger.Dim)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isActive(job)) {
                            LedgerButton("Stop", { viewModel.stopWatch(job.id) }, quiet = true)
                        }
                        LedgerButton("Discard", { viewModel.deleteWatch(job.id) }, danger = true)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 7: Run the tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`, `WatchModelsTest` green

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: bring Auto-record to the phone"
```

---

### Task 5: Save post and Record live

**Files:**
- Create: `app/src/main/java/com/stillhere/app/ui/save/SavePostScreen.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/record/RecordLiveScreen.kt`
- Test: `app/src/test/java/com/stillhere/app/ui/RelativeTimeTest.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/ledger/RelativeTime.kt`

**Interfaces:**
- Consumes: the primitives (Task 3), existing `AppViewModel` state (`urlInput`, `clipboardSuggestion`, `activeDownload`, `history`, `liveState`, `liveUsername`, `liveCheck`, `liveChecking`).
- Produces: `@Composable fun SavePostScreen(viewModel: AppViewModel)`; `@Composable fun RecordLiveScreen(viewModel: AppViewModel)`; `fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String`.

Both screens are restyled ports of what `AppUi.kt` already does, so nothing
native is lost: the clipboard banner, the active-download card and the local
history list all move across. Copy comes from the spec's table, verbatim.

`relativeTime` moves out of `AppUi.kt` into `ui/ledger/` because both screens use
it and it is the one piece of that file worth a unit test.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/stillhere/app/ui/RelativeTimeTest.kt`:

```kotlin
package com.stillhere.app.ui

import com.stillhere.app.ui.ledger.relativeTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_755_000_000_000L

    @Test
    fun `moments ago reads as just now`() {
        assertEquals("just now", relativeTime(now - 30_000, now))
    }

    @Test
    fun `minutes hours and days`() {
        assertEquals("5m ago", relativeTime(now - 5 * 60_000, now))
        assertEquals("3h ago", relativeTime(now - 3 * 3_600_000, now))
        assertEquals("2d ago", relativeTime(now - 2 * 86_400_000, now))
    }

    @Test
    fun `a clock skew into the future does not print a negative`() {
        assertEquals("just now", relativeTime(now + 60_000, now))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: FAIL — `Unresolved reference: relativeTime`

- [ ] **Step 3: Extract `relativeTime`**

Create `app/src/main/java/com/stillhere/app/ui/ledger/RelativeTime.kt`:

```kotlin
package com.stillhere.app.ui.ledger

/**
 * "5m ago" for the register's timestamps.
 *
 * [now] is a parameter so this is testable without a clock, and a timestamp in
 * the future — device clock skew, or a server ahead of the phone — reads as
 * "just now" rather than printing a negative.
 */
fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val elapsed = now - timestamp
    return when {
        elapsed < 60_000 -> "just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000}m ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000}h ago"
        else -> "${elapsed / 86_400_000}d ago"
    }
}
```

- [ ] **Step 4: Write `SavePostScreen`**

`app/src/main/java/com/stillhere/app/ui/save/SavePostScreen.kt`, built from the
primitives, with the spec's copy:

- `Eyebrow("Entry — saved post")`
- headline "Save a post before it's gone"
- `LedgerField("Link — TikTok or Instagram", …)`
- `LedgerButton("Save post", viewModel::downloadFromInput)`
- the clipboard banner as an `EntryCard` with `Stamp("Ready", StampKind.Filed)`,
  a "Use it" and a "Dismiss" `LedgerButton` — **kept, restyled**
- the active download as an `EntryCard` with `live = true` and the stamp from
  `stampFor`, mapping `DownloadProgress` to a status word:
  `Resolving`/`Requesting` → `"running"`, `Saving` → `"running"`,
  `Done` → `"finished"`, `Failed` → `"failed"`
- the ink is `seriesInk(platform)`, so an Instagram entry is written in the
  second ink exactly as `[data-series="ig"]` does on the web
- `FiledHead()` then the local history as `EntryCard`s, using `relativeTime`
- `LedgerEmpty("Nothing filed yet", "Paste a TikTok or Instagram link above.")`

- [ ] **Step 5: Write `RecordLiveScreen`**

`app/src/main/java/com/stillhere/app/ui/record/RecordLiveScreen.kt`:

- `Eyebrow("Entry — live capture · TikTok")`
- headline "Record a broadcast before it's gone"
- `LedgerField("Subject — username or live URL", …)`
- `LedgerButton("Begin capture", viewModel::startLiveRecording)` and a quiet
  `LedgerButton("Check if live", viewModel::checkLive)`
- the `LiveStatus` result and each `LiveState` variant (`Recording`, `Saved`,
  `Failed`) as `EntryCard`s with the matching stamp; `Recording` gets
  `live = true`, a `Stop` button, and the elapsed/bytes readout that
  `AppUi.kt` already computes
- `LedgerEmpty("Nothing filed yet", "Enter a TikTok username above.")`

Move `formatElapsed` and `humanBytes` from `AppUi.kt` into this file unchanged.

- [ ] **Step 6: Run the tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: rebuild Save post and Record live on the ledger"
```

---

### Task 6: The shell — bottom nav and a sessions sheet

**Files:**
- Create: `app/src/main/java/com/stillhere/app/ui/AppRoot.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/sessions/SessionsSheet.kt`
- Delete: `app/src/main/java/com/stillhere/app/ui/AppUi.kt`
- Delete: `app/src/main/java/com/stillhere/app/ui/theme/Brand.kt`
- Delete: `app/src/main/java/com/stillhere/app/ui/theme/` (directory)

**Interfaces:**
- Consumes: `SavePostScreen`, `RecordLiveScreen`, `AutoRecordScreen`, the primitives.
- Produces: `@Composable fun AppRoot(viewModel: AppViewModel)` — navigation only; `@Composable fun SessionsSheet(viewModel: AppViewModel, onDismiss: () -> Unit)`.

Three destinations, mirroring the web's tab strip so both products share one
mental model and no feature is phone-only or web-only:

| Destination | Screen |
|---|---|
| Record live | `RecordLiveScreen` |
| Auto-record | `AutoRecordScreen` |
| Save post | `SavePostScreen` |

Sessions move into a **modal bottom sheet** — a native idiom rather than the
web's side drawer — carrying the same two sections the web drawer has, plus the
backend URL and API key. One place for "how this app talks to my server", so
there is no separate Settings screen.

The masthead above the nav carries the wordmark and the tagline *"A register of
things published once"*.

- [ ] **Step 1: Write `SessionsSheet`**

A `ModalBottomSheet` with `shape = SquareCorners` and `containerColor = Ledger.Card`, containing:

- `FiledHead("Server")` then `LedgerField("Backend URL", …)` and
  `LedgerField("API key — optional", …)`, saved with `viewModel.saveSettings`.
  Keep the existing hint wording: *only set it if your server requires one*.
- `FiledHead("Sessions")` then a `TikTok session` row and an `Instagram session`
  row, each showing connected state as a `Stamp` and offering Sign in
  (`SessionLoginActivity`) / Sign out, driven by the existing
  `tiktokConnected` / `instagramConnected` / `logout` state.

- [ ] **Step 2: Write `AppRoot`**

```kotlin
package com.stillhere.app.ui

import ...

private enum class Destination(val label: String) {
    Record("Record live"),
    Watch("Auto-record"),
    Save("Save post"),
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    var destination by rememberSaveable { mutableStateOf(Destination.Save) }
    var sessionsOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Ledger.Board,
        topBar = { Masthead(onOpenSessions = { sessionsOpen = true }) },
        bottomBar = { LedgerNavBar(destination) { destination = it } },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (destination) {
                Destination.Record -> RecordLiveScreen(viewModel)
                Destination.Watch -> AutoRecordScreen(viewModel)
                Destination.Save -> SavePostScreen(viewModel)
            }
        }
    }

    if (sessionsOpen) SessionsSheet(viewModel) { sessionsOpen = false }
}
```

`LedgerNavBar` is a plain `Row` of three tappable labels above a hairline rule,
the active one in `Ledger.SeriesInk` with a 2dp underline — the web's tab strip,
not a Material `NavigationBar` with its pills and radius.

`Masthead` is the wordmark in `displaySmall`, the tagline in `LedgerType.label`,
and a Sessions button whose dot reads `Ledger.Filed` when both sessions are
connected — matching the web's single dot for two platforms.

- [ ] **Step 3: Delete the old UI**

```bash
git rm app/src/main/java/com/stillhere/app/ui/AppUi.kt
git rm app/src/main/java/com/stillhere/app/ui/theme/Brand.kt
```

`Brand.kt` carried `BrandGradient`, `HeroGlow`, `platformBrush`, `GradientButton`,
`PlatformBadge`, `PlatformChip` and `AppLogoBadge` — every one of them is a
dark-theme, rounded, gradient idiom with no place in the register. Confirm
nothing still imports them:

Run: `grep -rn "BrandGradient\|HeroGlow\|platformBrush\|GradientButton\|PlatformBadge\|PlatformChip\|AppLogoBadge" app/src`
Expected: no matches

- [ ] **Step 4: Run the tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: replace the dark shell with the register's three sections"
```

---

### Task 7: Adopting the async API

**Files:**
- Modify: `app/src/main/java/com/stillhere/app/data/SettingsRepo.kt`
- Modify: `app/src/main/java/com/stillhere/app/net/DownloadModels.kt`
- Modify: `app/src/main/java/com/stillhere/app/net/ApiClient.kt`
- Modify: `app/src/main/java/com/stillhere/app/download/DownloadManager.kt`
- Test: `app/src/test/java/com/stillhere/app/net/DownloadJobTest.kt`

**Interfaces:**
- Consumes: `POST /downloads?async=1`, `GET /downloads` from the server plan.
- Produces:
  - `SettingsRepo.DEFAULT_BASE_URL = "https://app.dioriza.com/stillhere"`
  - `DownloadJob` — `id`, `platform`, `status`, `url`, `error`, `files`, `fileUrls`, `zipUrl`; every field defaulted
  - `ApiClient.submitDownload(platform: Platform, url: String): DownloadJob`
  - `ApiClient.listDownloads(): List<DownloadJob>`
  - `ApiClient.awaitDownload(id: String, pollMs: Long = 2000, timeoutMs: Long = 900_000): DownloadJob`

Launching on `/stillhere` is what lets the server delete the `/tiktok` nginx
shim. Launching on the async door is what lets the server delete the synchronous
download path. Both are stated in the release notes in Task 9 so the server work
actually gets done.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/stillhere/app/net/DownloadJobTest.kt`:

```kotlin
package com.stillhere.app.net

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadJobTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a queued job as the async door returns it`() {
        val job = json.decodeFromString(
            DownloadJob.serializer(),
            """{"id":"20260815-101500-abc","platform":"tiktok_post","status":"queued",
               "url":"https://tiktok.com/x","error":null,"output_dir":"","files":[],
               "file_urls":[],"zip_url":null,"created_at":"2026-08-15T10:15:00+00:00",
               "started_at":null,"finished_at":null,"fetched_at":null}""",
        )

        assertEquals("queued", job.status)
        assertTrue(job.fileUrls.isEmpty())
        assertNull(job.zipUrl)
        assertTrue(job.isActive)
    }

    @Test
    fun `a finished instagram job carries a zip url`() {
        val job = json.decodeFromString(
            DownloadJob.serializer(),
            """{"id":"x","platform":"instagram","status":"finished",
               "files":["output/instagram/x/reel.mp4"],
               "file_urls":["/instagram/downloads/x/files/0"],
               "zip_url":"/instagram/downloads/x/zip"}""",
        )

        assertEquals(Platform.INSTAGRAM, job.resolvedPlatform)
        assertEquals("/instagram/downloads/x/zip", job.zipUrl)
        assertTrue(job.isTerminal)
    }

    @Test
    fun `an unknown platform does not throw`() {
        val job = json.decodeFromString(DownloadJob.serializer(), """{"id":"x","platform":"mystery"}""")

        assertNull(job.resolvedPlatform)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: FAIL — `Unresolved reference: DownloadJob`

- [ ] **Step 3: Add the model**

In `DownloadModels.kt`:

```kotlin
/**
 * Mirrors `DownloadJobResponse` — one row of the server's register, in any state.
 *
 * Separate from [DownloadResponse], which is the shape the *synchronous* door
 * returns and exists only for the previous app. Every field is defaulted so a
 * server-side shape change degrades rather than throws.
 */
@Serializable
data class DownloadJob(
    val id: String = "",
    val platform: String = "",
    val status: String = "",
    val url: String? = null,
    val error: String? = null,
    @SerialName("output_dir") val outputDir: String = "",
    val files: List<String> = emptyList(),
    @SerialName("file_urls") val fileUrls: List<String> = emptyList(),
    @SerialName("zip_url") val zipUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("fetched_at") val fetchedAt: String? = null,
) {
    val isActive: Boolean get() = status == "queued" || status == "running"
    val isTerminal: Boolean get() = status == "finished" || status == "failed"

    /** Null rather than a throw, so a platform we do not know about still lists. */
    val resolvedPlatform: Platform?
        get() = when (platform) {
            "instagram" -> Platform.INSTAGRAM
            "tiktok_post" -> Platform.TIKTOK
            else -> null
        }
}
```

- [ ] **Step 4: Add the client methods and switch `DownloadManager`**

```kotlin
    /** Submit a job and return at once with something to poll. */
    suspend fun submitDownload(platform: Platform, url: String): DownloadJob =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${requireBaseUrl()}${platform.path}?async=1")
                .post(buildRequestJson(url).toRequestBody(JSON_MEDIA))
                .applyApiKey()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw toApiException(response.code, text)
                json.decodeFromString(DownloadJob.serializer(), text)
            }
        }

    suspend fun listDownloads(): List<DownloadJob> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${requireBaseUrl()}/downloads")
            .get()
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiException(response.code, text)
            json.decodeFromString(ListSerializer(DownloadJob.serializer()), text)
        }
    }

    /**
     * Poll until the job reaches a terminal state.
     *
     * A dropped poll is not a failure — the server may be busy with two other
     * fetches — so transient errors are swallowed and only the timeout gives up.
     */
    suspend fun awaitDownload(
        id: String,
        pollMs: Long = 2_000,
        timeoutMs: Long = 900_000,
    ): DownloadJob {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val job = runCatching { listDownloads().firstOrNull { it.id == id } }.getOrNull()
            if (job != null && job.isTerminal) return job
            delay(pollMs)
        }
        throw ApiException("The download is taking longer than expected. Check the register on the web.")
    }
```

Add `import kotlinx.coroutines.delay`.

In `DownloadManager.download`, replace the single `api.createDownload(...)` call
with submit-then-await, keeping everything after it — the media filter, the
per-file MediaStore save, and the history write — exactly as it is:

```kotlin
            onProgress(DownloadProgress.Requesting(routed.platform))
            val submitted = api.submitDownload(routed.platform, routed.url)
            val job = api.awaitDownload(submitted.id)
            if (job.status == "failed") {
                return fail(job.error ?: "The download failed.", onProgress)
            }

            val mediaUrls = job.fileUrls.filterIndexed { index, _ ->
                isMediaFile(job.files.getOrNull(index))
            }
```

- [ ] **Step 5: Point at the new prefix**

In `SettingsRepo.kt`:

```kotlin
        // Pre-filled so the app works out of the box; change it in Sessions
        // anytime. Shipping on /stillhere is what lets the server drop the
        // /tiktok nginx shim.
        const val DEFAULT_BASE_URL = "https://app.dioriza.com/stillhere"
```

- [ ] **Step 6: Run the tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: submit downloads as jobs and poll the register"
```

---

### Task 8: The copy

**Files:**
- Modify: `app/src/main/java/com/stillhere/app/download/DownloadService.kt`
- Modify: `app/src/main/java/com/stillhere/app/live/LiveRecordingService.kt`
- Modify: `app/src/main/java/com/stillhere/app/ShareReceiverActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

The rule: **register voice inside the app, plain language on system surfaces.**
In-app text carries the web's vocabulary. Notifications, the share-target label
and permission rationales stay plain, because they appear out of context beside
other apps, where "Filing your entry" reads as a malfunction rather than a voice.

- [ ] **Step 1: Make the notifications plain**

In `DownloadService.describe`, replace the register-ish wording:

```kotlin
    // Deliberately plain. These strings appear in the shade next to every other
    // app on the phone, where the register's voice reads as a malfunction.
    private fun describe(progress: DownloadProgress): Triple<String, String, Boolean> = when (progress) {
        is DownloadProgress.Resolving -> Triple("Saving", "Reading the link", true)
        is DownloadProgress.Requesting ->
            Triple("Saving", "Saving from ${progress.platform.label}", true)
        is DownloadProgress.Saving ->
            Triple("Saving", "File ${progress.index} of ${progress.total}", true)
        is DownloadProgress.Done -> Triple("Saved to your gallery", "${progress.saved.size} file(s)", false)
        is DownloadProgress.Failed -> Triple("Download failed", progress.message, false)
    }
```

The failure text is the server's own message, unchanged — it is already written
for a person who pasted a link.

- [ ] **Step 2: Check the live service and share receiver**

`LiveRecordingService`'s notification text follows the same rule: plain. The
share-sheet toast keeps its plain wording, and the share target's label is
`@string/app_name`, which Task 1 already set to `Still Here`.

- [ ] **Step 3: Confirm the in-app copy matches the spec table**

Run: `grep -rn "Entry — \|Begin capture\|Place the order\|Save post\|Nothing filed yet\|Subject — \|Link — \|Duration in seconds" app/src/main/java/com/stillhere/app/ui`

Expected: every phrase in the spec's in-app copy table appears exactly once per
screen that owns it, spelled as the table spells it — including the em dashes
and the lowercase after them.

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "copy: register voice in the app, plain language in the shade"
```

---

### Task 9: Release notes that trigger the server work

**Files:**
- Modify: `README.md`
- Create: `docs/release-notes-1.0.md`

Nothing is retired on the client. This app shipping is the trigger for retiring
**two server-side shims**, and unless that is written in the release notes it
will not happen.

- [ ] **Step 1: Write the release notes**

Create `docs/release-notes-1.0.md`:

```markdown
# Still Here 1.0

A rebuild of TTL Downloader as *Still Here* — a paper register, selaras with the
web app at app.dioriza.com/stillhere.

## What is new

- **Auto-record.** Place a standing order for a creator who is not live yet; the
  server watches and starts recording the moment they go live. This existed on
  the web only.
- **Three sections**, matching the web: Record live, Auto-record, Save post.
- **Sessions and server settings in one place** — a bottom sheet, not a separate
  screen.
- **Concurrent downloads.** Submissions are queued as jobs on the server; two
  run at a time.

## What is unchanged

Share-sheet receiving, saving into the phone's gallery, clipboard detection,
local history, and downloads that survive leaving the app.

## Upgrading

This is a **new app** to Android (`com.stillhere.app`), not an update to
`com.ttldownloader.app`. That was a deliberate choice:

- The old app stays installed as a separate icon and must be removed by hand.
- The backend URL, API key and local history do **not** carry over. Re-enter
  them under Sessions.

## This release retires two server shims

Both exist only because the previous app is in production. Once 1.0 is
installed, delete them — the triggers are recorded in `SSH.md` in the server
repo:

1. **The `/tiktok` nginx prefix.** 1.0 defaults to `/stillhere`.
2. **The synchronous download door** (`POST /downloads` with no `?async=1`).
   1.0 submits jobs and polls.
```

- [ ] **Step 2: Update the README**

Replace the product name and the two facts that changed: the app is *Still Here*,
and the default backend is `https://app.dioriza.com/stillhere`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "docs: release notes for Still Here 1.0"
```

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
|---|---|
| `LedgerTheme`, token layer, zero radius, 1.5dp rules | 2 |
| Fonts bundled, subset, OFL | 2 (documented drop-in — see deviation below) |
| `ui/ledger/` primitives table, all seven | 3 |
| Bottom navigation, three destinations | 6 |
| Sessions as a modal bottom sheet, URL + key inside it | 6 |
| Share-sheet, MediaStore, clipboard, history, background service kept | 5, 8 |
| Code structure: `ui/{ledger,record,watch,save,sessions}`, `AppRoot.kt` navigation only | 4, 5, 6 |
| Auto-record: models, client, screen, stamps — scheduled first | 4 |
| Base URL default `/stillhere` | 7 |
| Downloads use the async job API | 7 |
| `X-API-Key` needs no client change | — (already present; verified in Task 7's client methods, which all call `applyApiKey()`) |
| Naming table: applicationId, namespace, app_name, versionCode/Name | 1 |
| Copy: in-app register voice | 4, 5 |
| Copy: system surfaces plain | 8 |
| Testing: primitives | 3 |
| Testing: share-sheet and clipboard survive | 5 (ported), 3 (primitives they render with) |
| Release notes state the two shim triggers | 9 |

**Deviations, both deliberate:**

1. **The three OFL fonts are not bundled in this plan; the type system is built
   to take them.** Fetching font binaries is not something this plan can do
   unattended, so `LedgerType` resolves to the platform's serif and monospace,
   with every size, weight and letter-spacing already correct. `docs/fonts.md`
   makes adding them a four-line change in one file. This is the one visible gap
   between the plan's output and the spec.
2. **Compose UI tests are written as instrumented tests** (`androidTest`), which
   is what `createComposeRule` requires. They compile in CI via
   `assembleDebugAndroidTest` but need a device or emulator to execute. The
   logic worth asserting without a device — the stamp vocabulary, the tokens,
   relative time, and every wire model — is covered by JVM unit tests that run
   on every build.

**Two testing items from the spec cannot be automated here and are manual
smoke tests on a device:** that MediaStore saving still lands files in the
gallery, and that a download continues when the app is backgrounded. Both are
unchanged code paths, but both are worth ten seconds each before release.

**Out of scope, per the spec:** dark mode, batch multi-URL paste, a home-screen
widget, iOS, and any change to the web app.
