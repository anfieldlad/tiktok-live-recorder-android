# Still Here Local Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the app into two product flavors: `server` (today's app, unchanged in behaviour) and `local`, which saves TikTok, Instagram and YouTube posts with yt-dlp running on the phone and no backend at all.

**Architecture:** Shared code in `src/main` depends on two seams: `MediaSource` (link → media files) and `SessionStore` (where a login cookie lives). It also depends on a `Flavor` contract that each flavor's `FlavorModule` fulfils. Everything that talks to the backend moves to `src/server`. The on-device engine lives in `src/local`: yt-dlp through `youtubedl-android`, plus tikwm and an Instagram API extractor as Kotlin fallbacks. The rule is that if `src/local` compiles, the local app contains no backend code.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (Material 3), AGP 8.9.1, OkHttp 4.12, kotlinx.serialization, DataStore, coroutines, `io.github.junkfood02.youtubedl-android` (library + ffmpeg), JUnit 4, OkHttp MockWebServer.

**Spec:** `docs/superpowers/specs/2026-09-24-local-flavor-design.md`

## Global Constraints

- `minSdk 26`, `compileSdk 36`, `targetSdk 36`, JDK 17 (`JAVA_HOME` must point at a JDK 17+; Java 8 fails with this AGP).
- Flavor dimension `engine`. `server` keeps `applicationId` `com.stillhere.app` with no suffix, so existing installs keep upgrading. `local` gets `applicationIdSuffix = ".local"` and the app name **Still Here Local**.
- Nothing under `app/src/main` or `app/src/local` may import `com.stillhere.app.net` or reference `ApiClient`, `SettingsRepo`, `LiveController` or `WatchJob`.
- The local flavor never sends a session cookie anywhere except the platform it belongs to.
- tikwm is only a fallback: TikTok photo posts, or when yt-dlp fails on a TikTok link.
- yt-dlp arguments are the server's (`post_download_service.py`, `instagram_download_service.py`) minus `--impersonate`.
- Notification copy stays plain. In-app copy follows the register's voice, as in the existing screens.
- Commit messages follow the repo's style (`feat:`, `refactor:`, `test:`, `docs:`, lowercase summary) and end with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Run Gradle from the repo root with `./gradlew` (Git Bash) or `.\gradlew.bat` (PowerShell).
- Until Task 5 adds flavors, the test task is `:app:testDebugUnitTest`. From Task 5 on it is `:app:testServerDebugUnitTest` / `:app:testLocalDebugUnitTest`.

## File Map

**`app/src/main` (shared):**
- `domain/Platform.kt`: new. `Platform` enum without the backend `path`.
- `domain/MediaFiles.kt`: new. `isMedia(name)`, the sidecar filter both flavors use.
- `download/MediaSource.kt`: new. `MediaSource`, `IncomingFile`, `MediaException`.
- `auth/SessionStore.kt`: new. The `SessionStore` interface.
- `Flavor.kt`, `Features.kt`: new. The flavor contract and what a flavor offers.
- `ui/Destination.kt`: new. The `Destination` enum, moved out of `AppRoot`.
- `download/DownloadManager.kt`, `auth/SessionLoginActivity.kt`, `ui/AppViewModel.kt`, `ui/AppRoot.kt`, `ui/sessions/SessionsSheet.kt`, `StillHereApp.kt`, `AndroidManifest.xml`: modified.

**`app/src/server` (moved from main, plus a few new files):**
- `net/*`: `ApiClient` and its models (moved), plus new `ServerMediaSource.kt` and `ServerSessionStore.kt`.
- `data/SettingsRepo.kt`, `live/*`, `ui/record/*`, `ui/watch/*`: moved.
- New: `live/LiveUsername.kt`, `ui/record/LiveViewModel.kt`, `ui/watch/WatchViewModel.kt`, `ui/sessions/ServerSettingsSection.kt`, `ui/sessions/ServerSettingsViewModel.kt`, `FlavorModule.kt`, `FlavorFeatures.kt`, `ui/FlavorScreens.kt`, `AndroidManifest.xml`.

**`app/src/local` (new):**
- `FlavorModule.kt`, `FlavorFeatures.kt`, `ui/FlavorScreens.kt`, `AndroidManifest.xml`
- `res/values/strings.xml`, `res/drawable/ic_launcher_background.xml`
- `auth/LocalSessionStore.kt`
- In `local/`: `SessionCookies.kt`, `LocalRoute.kt`, `YtDlpArgs.kt`, `FriendlyError.kt`, `InstagramShortcode.kt`, `InstagramMediaParser.kt`, `TikwmParser.kt`, `RemoteMedia.kt`, `PublicUrl.kt`, `MediaExtension.kt`, `RemoteDownloader.kt`, `YtDlp.kt`, `LocalMediaSource.kt`, `Engines.kt`, `UpdatePolicy.kt`, `YtDlpUpdater.kt`, `ui/YtDlpSection.kt`

**Tests:** `app/src/test` (shared), `app/src/testServer`, `app/src/testLocal`.

---

### Task 1: Device spike for youtubedl-android

This runs on a throwaway branch. Only the notes file is kept.

**Needs a device.** Either plug in a phone with USB debugging enabled, or install an emulator (`sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"`, then create an AVD in Android Studio). `adb devices` must list a device before starting. If none can be made available, stop and tell the user: every later task depends on this answer.

**Files (spike branch only):**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `app/src/androidTest/java/com/stillhere/app/spike/YtDlpSpikeTest.kt`
- Create (kept): `docs/superpowers/plans/2026-09-24-local-flavor-spike.md`

**Interfaces:**
- Produces: the confirmed `youtubedl-android` version and API names, which Tasks 10 and 13 use (`YoutubeDL.getInstance().init/execute/versionName/updateYoutubeDL`, `YoutubeDL.UpdateChannel`, `YoutubeDL.UpdateStatus`, `YoutubeDLException`, `FFmpeg.getInstance().init`).

- [ ] **Step 1: Branch off**

```bash
git switch -c spike/ytdlp
```

- [ ] **Step 2: Pick the library version**

Open https://central.sonatype.com/artifact/io.github.junkfood02.youtubedl-android/library and note the newest release. Use it everywhere below (the plan writes `0.17.4`; replace it if the newest release is different).

- [ ] **Step 3: Add the dependency**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
youtubedlAndroid = "0.17.4"
```

Under `[libraries]` add:

```toml
youtubedl-android-library = { group = "io.github.junkfood02.youtubedl-android", name = "library", version.ref = "youtubedlAndroid" }
youtubedl-android-ffmpeg = { group = "io.github.junkfood02.youtubedl-android", name = "ffmpeg", version.ref = "youtubedlAndroid" }
```

In `app/build.gradle.kts`, inside `android { }` after `buildFeatures { }`, add:

```kotlin
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
```

In `dependencies { }` add:

```kotlin
    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
```

- [ ] **Step 4: Write the spike test**

`app/src/androidTest/java/com/stillhere/app/spike/YtDlpSpikeTest.kt`:

```kotlin
package com.stillhere.app.spike

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Throwaway: answers "does yt-dlp work on this phone without --impersonate?" */
@RunWith(AndroidJUnit4::class)
class YtDlpSpikeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val args = InstrumentationRegistry.getArguments()

    @Before
    fun init() {
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
    }

    private fun fetch(url: String, extra: List<Pair<String, String?>>): List<File> {
        val dir = File(context.cacheDir, "spike-${System.nanoTime()}").apply { mkdirs() }
        val request = YoutubeDLRequest(url).apply {
            addOption("--check-formats")
            addOption("--paths", dir.absolutePath)
            addOption("--output", "%(title).80s-%(id)s.%(ext)s")
            extra.forEach { (name, value) -> if (value == null) addOption(name) else addOption(name, value) }
        }
        val started = System.currentTimeMillis()
        val response = YoutubeDL.getInstance().execute(request)
        Log.i(TAG, "exit=${response.exitCode} in ${System.currentTimeMillis() - started}ms\n${response.err}")
        return dir.walkTopDown().filter { it.isFile }.toList().onEach { Log.i(TAG, "file ${it.name} ${it.length()}") }
    }

    @Test
    fun reportsVersion() {
        Log.i(TAG, "yt-dlp ${YoutubeDL.getInstance().versionName(context)}")
    }

    @Test
    fun tiktokVideoWithoutImpersonate() {
        val files = fetch(
            args.getString("tiktokUrl")!!,
            listOf("--no-playlist" to null, "--add-header" to "Referer: https://www.tiktok.com/"),
        )
        assertTrue(files.any { it.extension == "mp4" })
    }

    @Test
    fun instagramReel() {
        val files = fetch(
            args.getString("instagramUrl")!!,
            listOf("--add-header" to "Referer: https://www.instagram.com/"),
        )
        assertTrue(files.any { it.extension == "mp4" })
    }

    @Test
    fun youtubeShortMergesToMp4() {
        val files = fetch(
            args.getString("youtubeUrl")!!,
            listOf(
                "--no-playlist" to null,
                "-f" to "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b",
                "--merge-output-format" to "mp4",
            ),
        )
        assertTrue(files.any { it.extension == "mp4" })
    }

    @Test
    fun updateApiWorks() {
        val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
        Log.i(TAG, "update status=$status now ${YoutubeDL.getInstance().versionName(context)}")
    }

    private companion object {
        const val TAG = "YtDlpSpike"
    }
}
```

If `YoutubeDL.UpdateChannel._STABLE` doesn't compile, look at the library's `YoutubeDL` class in Android Studio for the stable-channel constant and use that. **Write down the name you used.**

- [ ] **Step 5: Run it on the device**

Pick one current, public link of each kind: a TikTok video, an Instagram reel, a YouTube Short.

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.stillhere.app.spike.YtDlpSpikeTest \
  "-Pandroid.testInstrumentationRunnerArguments.tiktokUrl=<tiktok video url>" \
  "-Pandroid.testInstrumentationRunnerArguments.instagramUrl=<instagram reel url>" \
  "-Pandroid.testInstrumentationRunnerArguments.youtubeUrl=<youtube short url>"
```

Then read the log:

```bash
adb logcat -d -s YtDlpSpike
```

- [ ] **Step 6: Measure the APK**

```bash
./gradlew :app:assembleDebug
ls -la app/build/outputs/apk/debug/
```

- [ ] **Step 7: Write the notes and leave the branch**

Create `docs/superpowers/plans/2026-09-24-local-flavor-spike.md` recording:
- the device or emulator model and its Android version
- the library version
- the yt-dlp version reported, both before and after the update
- pass or fail for each test, the time taken, and yt-dlp's stderr when a test failed
- the exact stable-channel constant name, and the `UpdateStatus` values
- the APK size

Commit everything on the spike branch, go back, and bring over only the notes file:

```bash
git add -A && git commit -m "spike: youtubedl-android on a device"
git switch -
git checkout spike/ytdlp -- docs/superpowers/plans/2026-09-24-local-flavor-spike.md
git commit -m "docs: record the youtubedl-android device spike"
```

The spike branch is never merged.

**Gate:** if `tiktokVideoWithoutImpersonate` failed, **stop** and bring the notes to the user. The spec's decision 3 needs revisiting before any further task.

---

### Task 2: Move `Platform` into `domain` and drop its backend path

**Files:**
- Create: `app/src/main/java/com/stillhere/app/domain/Platform.kt`
- Modify: `app/src/main/java/com/stillhere/app/net/DownloadModels.kt` (delete the `Platform` enum)
- Modify: `app/src/main/java/com/stillhere/app/net/ApiClient.kt` (add `downloadPath`, import `Platform`)
- Modify imports in: `auth/SessionLoginActivity.kt`, `domain/UrlRouter.kt`, `download/DownloadManager.kt`, `ui/AppViewModel.kt`, `ui/ledger/Tokens.kt`, `ui/save/SavePostScreen.kt`, `ui/sessions/SessionsSheet.kt`, `test/.../domain/UrlRouterTest.kt`, `test/.../ui/ledger/TokensTest.kt`

**Interfaces:**
- Produces: `com.stillhere.app.domain.Platform` with `TIKTOK`, `INSTAGRAM`, `YOUTUBE` and `val label: String`. There is no `path` any more.

- [ ] **Step 1: Create the new enum**

`app/src/main/java/com/stillhere/app/domain/Platform.kt`:

```kotlin
package com.stillhere.app.domain

/** The three platforms Still Here saves from. Routing is decided from a link's hostname. */
enum class Platform(val label: String) {
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
}
```

- [ ] **Step 2: Delete the old enum**

In `net/DownloadModels.kt`, delete the KDoc and the `enum class Platform(...) { ... }` block at the top of the file (lines 6–17).

- [ ] **Step 3: Give `ApiClient` its own path table**

In `net/ApiClient.kt`, add `import com.stillhere.app.domain.Platform` to the imports. Replace both occurrences of `platform.path` with `downloadPath(platform)`. Add this private function next to `authStatusPath`:

```kotlin
    // The same endpoint for TikTok and YouTube on purpose: the server routes on
    // the link's hostname, so YouTube never needed an address of its own.
    private fun downloadPath(platform: Platform): String = when (platform) {
        Platform.TIKTOK, Platform.YOUTUBE -> "/downloads"
        Platform.INSTAGRAM -> "/instagram/downloads"
    }
```

- [ ] **Step 4: Rewrite the imports**

```bash
grep -rl "import com.stillhere.app.net.Platform" app/src | xargs sed -i 's/import com.stillhere.app.net.Platform/import com.stillhere.app.domain.Platform/'
sed -i '/import com.stillhere.app.domain.Platform/d' app/src/main/java/com/stillhere/app/domain/UrlRouter.kt app/src/test/java/com/stillhere/app/domain/UrlRouterTest.kt
grep -rn "Platform" app/src/main/java/com/stillhere/app/net/*.kt | grep -v ApiClient.kt
```

Any file the last command lists uses `Platform` from the `net` package without an import. Add `import com.stillhere.app.domain.Platform` to each of them.

- [ ] **Step 5: Build and test**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, and all existing tests pass. `UrlRouterTest` and `TokensTest` still pass unchanged.

- [ ] **Step 6: Commit**

```bash
git add -A app/src
git commit -m "refactor: move Platform to domain, keep the server's paths in ApiClient"
```

---

### Task 3: Introduce the `MediaSource` and `SessionStore` seams

In this task the server implementations still live in `src/main`. Task 5 moves them out.

**Files:**
- Create: `app/src/main/java/com/stillhere/app/domain/MediaFiles.kt`
- Create: `app/src/main/java/com/stillhere/app/download/MediaSource.kt`
- Create: `app/src/main/java/com/stillhere/app/auth/SessionStore.kt`
- Create: `app/src/main/java/com/stillhere/app/Flavor.kt`
- Create: `app/src/main/java/com/stillhere/app/FlavorModule.kt` (the server version; moves in Task 5)
- Create: `app/src/main/java/com/stillhere/app/net/ServerMediaSource.kt`
- Create: `app/src/main/java/com/stillhere/app/net/ServerSessionStore.kt`
- Modify: `StillHereApp.kt`, `download/DownloadManager.kt`, `auth/SessionLoginActivity.kt`, `ui/AppViewModel.kt`, `live/LiveRecordingService.kt`
- Test: `app/src/test/java/com/stillhere/app/domain/MediaFilesTest.kt`

**Interfaces:**
- Consumes: `domain.Platform` (Task 2).
- Produces:
  - `MediaFiles.isMedia(name: String?): Boolean`
  - `data class IncomingFile(val index: Int, val total: Int, val name: String, val contentType: String?)`
  - `interface MediaSource { suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit) }`
  - `class MediaException(message: String) : Exception(message)`
  - `interface SessionStore { suspend fun isAvailable(): Boolean; suspend fun isConnected(platform: Platform): Boolean; suspend fun save(platform: Platform, sessionId: String); suspend fun clear(platform: Platform) }`
  - `interface Flavor { val mediaSource: MediaSource; val sessionStore: SessionStore; fun onAppStart() {} }` (Task 4 adds `features`)
  - `StillHereApp.flavor: FlavorModule`. The server `FlavorModule` exposes `settings: SettingsRepo`, `api: ApiClient` and `liveController: LiveController`.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/stillhere/app/domain/MediaFilesTest.kt`:

```kotlin
package com.stillhere.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFilesTest {

    @Test
    fun keepsMedia() {
        assertTrue(MediaFiles.isMedia("clip-7300000000000000000.mp4"))
        assertTrue(MediaFiles.isMedia("/downloads/abc/files/image-001.JPG"))
    }

    @Test
    fun dropsSidecarsAndLeftovers() {
        listOf("a.info.json", "meta.JSON", "notes.txt", "x.nfo", "v.mp4.part", "v.mp4.ytdl").forEach {
            assertFalse(it, MediaFiles.isMedia(it))
        }
    }

    @Test
    fun dropsNothing() {
        assertFalse(MediaFiles.isMedia(null))
        assertFalse(MediaFiles.isMedia(""))
        assertFalse(MediaFiles.isMedia("some/dir/"))
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.stillhere.app.domain.MediaFilesTest"`
Expected: compilation FAILS with `Unresolved reference: MediaFiles`.

- [ ] **Step 3: Implement `MediaFiles`**

`app/src/main/java/com/stillhere/app/domain/MediaFiles.kt`:

```kotlin
package com.stillhere.app.domain

/** Real media we want in the gallery — not the sidecars and leftovers extractors write. */
object MediaFiles {
    private val NOT_MEDIA = listOf(".json", ".txt", ".nfo", ".part", ".ytdl")

    fun isMedia(name: String?): Boolean {
        val file = name?.substringAfterLast('/')?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
        return NOT_MEDIA.none { file.endsWith(it) }
    }
}
```

- [ ] **Step 4: Run the test to see it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.stillhere.app.domain.MediaFilesTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Create the seams**

`app/src/main/java/com/stillhere/app/download/MediaSource.kt`:

```kotlin
package com.stillhere.app.download

import com.stillhere.app.domain.UrlRouter
import java.io.InputStream

/** One media file on its way to the gallery. [index] is 1-based. */
data class IncomingFile(
    val index: Int,
    val total: Int,
    val name: String,
    val contentType: String?,
)

/**
 * Turns a routed link into media files. The two flavors differ here: `server`
 * asks the backend, `local` runs the extraction on the phone.
 *
 * [onFile] is called once per file, in order, on an IO thread, while that file's
 * stream is open; the stream is closed when it returns. Failures are thrown as
 * [MediaException] with a message written for the person who shared the link.
 */
interface MediaSource {
    suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit)
}

/** A failure whose message is fit to show as-is. */
class MediaException(message: String) : Exception(message)
```

`app/src/main/java/com/stillhere/app/auth/SessionStore.kt`:

```kotlin
package com.stillhere.app.auth

import com.stillhere.app.domain.Platform

/** Where a platform's login cookie lives: on the backend, or only on this phone. */
interface SessionStore {
    /** Whether sessions can be managed right now. The server flavor needs a backend URL first. */
    suspend fun isAvailable(): Boolean

    /** Whether [platform] has a stored session. Never throws; an unreachable store reads as false. */
    suspend fun isConnected(platform: Platform): Boolean

    suspend fun save(platform: Platform, sessionId: String)

    suspend fun clear(platform: Platform)
}
```

`app/src/main/java/com/stillhere/app/Flavor.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.auth.SessionStore
import com.stillhere.app.download.MediaSource

/**
 * The contract each flavor's `FlavorModule` fulfils. Shared code uses only these
 * members; flavor code reaches its own extras through [StillHereApp.flavor], whose
 * static type is that flavor's `FlavorModule`.
 */
interface Flavor {
    val mediaSource: MediaSource
    val sessionStore: SessionStore

    /** Called once from [StillHereApp.onCreate]. */
    fun onAppStart() {}
}
```

- [ ] **Step 6: Create the server implementations**

`app/src/main/java/com/stillhere/app/net/ServerMediaSource.kt`:

```kotlin
package com.stillhere.app.net

import com.stillhere.app.domain.MediaFiles
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import com.stillhere.app.download.MediaSource
import java.io.InputStream

/** Asks the backend to fetch a link, then streams back each file it produced. */
class ServerMediaSource(private val api: ApiClient) : MediaSource {

    override suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit) {
        try {
            // Submit and poll rather than holding one long request open. This
            // is what lets the server delete its synchronous download door.
            val submitted = api.submitDownload(routed.platform, routed.url)
            val job = api.awaitDownload(submitted.id)
            if (job.status == "failed") throw MediaException(job.error ?: "The download failed.")

            // The backend lists metadata sidecars (.json / .info.json) alongside the media.
            // We must NOT fetch those: the Instagram endpoint wipes the whole download once
            // the last *media* file has been served, so a trailing .json fetch 404s and would
            // fail the entire job. The parallel `files` array tells us each file_url's name.
            val mediaUrls = job.fileUrls.filterIndexed { index, _ ->
                MediaFiles.isMedia(job.files.getOrNull(index))
            }
            if (mediaUrls.isEmpty()) {
                throw MediaException("The server returned no downloadable media for that link.")
            }

            mediaUrls.forEachIndexed { index, fileUrl ->
                api.withFile(fileUrl) { name, type, stream ->
                    onFile(IncomingFile(index + 1, mediaUrls.size, name, type), stream)
                }
            }
        } catch (e: ApiException) {
            throw MediaException(e.message ?: "Download failed.")
        }
    }
}
```

`app/src/main/java/com/stillhere/app/net/ServerSessionStore.kt`:

```kotlin
package com.stillhere.app.net

import com.stillhere.app.auth.SessionStore
import com.stillhere.app.data.SettingsRepo
import com.stillhere.app.domain.Platform

/** Sessions live on the backend; this app only hands them over. */
class ServerSessionStore(
    private val api: ApiClient,
    private val settings: SettingsRepo,
) : SessionStore {

    override suspend fun isAvailable(): Boolean = settings.baseUrl().isNotBlank()

    override suspend fun isConnected(platform: Platform): Boolean =
        runCatching { api.authStatus(platform) }.getOrDefault(false)

    override suspend fun save(platform: Platform, sessionId: String) = api.saveSession(platform, sessionId)

    override suspend fun clear(platform: Platform) = api.clearSession(platform)
}
```

`app/src/main/java/com/stillhere/app/FlavorModule.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.auth.SessionStore
import com.stillhere.app.data.SettingsRepo
import com.stillhere.app.download.MediaSource
import com.stillhere.app.live.LiveController
import com.stillhere.app.net.ApiClient
import com.stillhere.app.net.ServerMediaSource
import com.stillhere.app.net.ServerSessionStore

/** The server flavor: every download, session and live stream goes through the backend. */
class FlavorModule(app: StillHereApp) : Flavor {
    val settings: SettingsRepo by lazy { SettingsRepo(app) }
    val api: ApiClient by lazy { ApiClient(settings) }

    /** Process-wide holder for the active live recording. */
    val liveController: LiveController by lazy { LiveController() }

    override val mediaSource: MediaSource by lazy { ServerMediaSource(api) }
    override val sessionStore: SessionStore by lazy { ServerSessionStore(api, settings) }
}
```

- [ ] **Step 7: Rewire `StillHereApp`**

Replace the body of `StillHereApp.kt` with:

```kotlin
package com.stillhere.app

import android.app.Application
import com.stillhere.app.data.HistoryRepo
import com.stillhere.app.data.MediaStoreSaver
import com.stillhere.app.download.DownloadController
import com.stillhere.app.download.DownloadManager

/**
 * Tiny manual DI container. The app is small enough that lazy singletons on the
 * Application beat pulling in a DI framework. Reach them via
 * `(context.applicationContext as StillHereApp)`.
 */
class StillHereApp : Application() {
    /** This build's flavor: the backend client or the on-device engine. */
    val flavor: FlavorModule by lazy { FlavorModule(this) }

    val history: HistoryRepo by lazy { HistoryRepo(this) }
    val saver: MediaStoreSaver by lazy { MediaStoreSaver(this) }
    val downloadManager: DownloadManager by lazy { DownloadManager(flavor.mediaSource, saver, history) }

    /** Process-wide holder for the currently active download's progress. */
    val controller: DownloadController by lazy { DownloadController() }

    override fun onCreate() {
        super.onCreate()
        flavor.onAppStart()
    }
}
```

- [ ] **Step 8: Rewire `DownloadManager`**

Replace the `DownloadManager` class, keeping `DownloadProgress` above it unchanged. Remove the imports of `ApiClient` and `ApiException`:

```kotlin
/**
 * Orchestrates one end-to-end download: resolve the shared text to a platform URL,
 * hand it to this flavor's [MediaSource], and stream each file it yields into the
 * gallery — emitting [DownloadProgress] along the way.
 */
class DownloadManager(
    private val source: MediaSource,
    private val saver: MediaStoreSaver,
    private val history: HistoryRepo,
) {
    suspend fun download(sharedText: String?, onProgress: (DownloadProgress) -> Unit): DownloadProgress {
        onProgress(DownloadProgress.Resolving)
        val routed = UrlRouter.route(sharedText)
            ?: return fail("That doesn't look like a TikTok, Instagram or YouTube link.", onProgress)

        return try {
            onProgress(DownloadProgress.Requesting(routed.platform))
            val saved = mutableListOf<SavedItem>()
            source.fetch(routed) { file, stream ->
                onProgress(DownloadProgress.Saving(routed.platform, file.index, file.total))
                saved += saver.save(file.name, file.contentType, stream)
            }
            if (saved.isEmpty()) return fail("Nothing to save came back for that link.", onProgress)

            history.add(
                HistoryEntry(
                    url = routed.url,
                    platform = routed.platform.label,
                    fileCount = saved.size,
                    timestamp = System.currentTimeMillis(),
                )
            )

            DownloadProgress.Done(routed.platform, saved).also(onProgress)
        } catch (e: MediaException) {
            fail(e.message ?: "Download failed.", onProgress)
        } catch (e: Exception) {
            fail(e.message ?: "Unexpected error.", onProgress)
        }
    }

    private fun fail(message: String, onProgress: (DownloadProgress) -> Unit): DownloadProgress =
        DownloadProgress.Failed(message).also(onProgress)
}
```

The imports this file needs are `HistoryEntry`, `HistoryRepo`, `MediaStoreSaver`, `SavedItem`, `domain.Platform` and `domain.UrlRouter`. `MediaSource` and `MediaException` are in the same package. The old `isMediaFile` is gone because `ServerMediaSource` now uses `MediaFiles`. The "doesn't look like" message now names YouTube, which the router has supported since `680dcff`.

- [ ] **Step 9: Rewire `SessionLoginActivity`**

In `saveAndFinish`, replace `app.api.saveSession(platform, sessionId)` with `app.flavor.sessionStore.save(platform, sessionId)`. Replace the failure toast text `"Captured login but couldn't reach the backend."` with `"Captured the login but couldn't save it."`.

- [ ] **Step 10: Point the remaining callers at `flavor`**

```bash
sed -i 's/app\.api\./app.flavor.api./g; s/app\.liveController/app.flavor.liveController/g; s/app\.settings\./app.flavor.settings./g' \
  app/src/main/java/com/stillhere/app/ui/AppViewModel.kt \
  app/src/main/java/com/stillhere/app/live/LiveRecordingService.kt
```

Then, in `AppViewModel.kt`, replace `refreshAuthStatus()` and `logout()` so they go through the seam:

```kotlin
    /** Refresh per-platform login status from this flavor's session store. */
    fun refreshAuthStatus() {
        viewModelScope.launch {
            val sessions = app.flavor.sessionStore
            if (!sessions.isAvailable()) return@launch
            _tiktokConnected.value = sessions.isConnected(Platform.TIKTOK)
            _instagramConnected.value = sessions.isConnected(Platform.INSTAGRAM)
        }
    }

    fun logout(platform: Platform) {
        viewModelScope.launch {
            runCatching { app.flavor.sessionStore.clear(platform) }
            refreshAuthStatus()
        }
    }
```

- [ ] **Step 11: Build and test**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, all tests pass.

Run: `grep -rn "app\.api\b\|app\.settings\b\|app\.liveController" app/src/main`
Expected: no output.

- [ ] **Step 12: Commit**

```bash
git add -A app/src
git commit -m "refactor: put downloads and sessions behind MediaSource and SessionStore"
```

---

### Task 4: Split flavor-only state out of the shared UI

`AppViewModel`, `AppRoot` and `SessionsSheet` stop knowing about live recording, auto-record and the backend settings. In this task everything still lives in `src/main`.

**Files:**
- Create: `app/src/main/java/com/stillhere/app/ui/Destination.kt`
- Create: `app/src/main/java/com/stillhere/app/Features.kt`
- Create: `app/src/main/java/com/stillhere/app/FlavorFeatures.kt` (the server version; moves in Task 5)
- Create: `app/src/main/java/com/stillhere/app/ui/FlavorScreens.kt` (the server version; moves in Task 5)
- Create: `app/src/main/java/com/stillhere/app/live/LiveUsername.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/record/LiveViewModel.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/watch/WatchViewModel.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/sessions/ServerSettingsSection.kt`
- Create: `app/src/main/java/com/stillhere/app/ui/sessions/ServerSettingsViewModel.kt`
- Modify: `Flavor.kt`, `FlavorModule.kt`, `ui/AppViewModel.kt`, `ui/AppRoot.kt`, `ui/sessions/SessionsSheet.kt`, `ui/record/RecordLiveScreen.kt`, `ui/watch/AutoRecordScreen.kt`, `app/src/screenshotTest/java/com/stillhere/app/ui/AppShellPreviews.kt`
- Test: `app/src/test/java/com/stillhere/app/live/LiveUsernameTest.kt`

**Interfaces:**
- Consumes: `Flavor`, `SessionStore`, `StillHereApp.flavor` (Task 3).
- Produces:
  - `enum class Destination(val label: String) { Record, Watch, Save }` (public, in `com.stillhere.app.ui`)
  - `data class Features(val destinations: List<Destination>, val sessionsNote: String)`
  - `Flavor.features: Features`
  - top-level `val flavorFeatures: Features` in package `com.stillhere.app`, one per flavor
  - `@Composable fun FlavorDestination(destination: Destination)` and `@Composable fun FlavorSettingsSection(onDone: () -> Unit)` in package `com.stillhere.app.ui`, one of each per flavor
  - `fun normalizeLiveUsername(raw: String): String` in package `com.stillhere.app.live`
  - `AppViewModel.sessionsAvailable: StateFlow<Boolean>`, `AppViewModel.features: Features`
  - `LedgerNavBar(entries: List<Destination>, current: Destination, onSelect: (Destination) -> Unit)`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/stillhere/app/live/LiveUsernameTest.kt`:

```kotlin
package com.stillhere.app.live

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveUsernameTest {

    @Test
    fun bareHandle() = assertEquals("someone", normalizeLiveUsername("someone"))

    @Test
    fun atHandleWithSpaces() = assertEquals("someone", normalizeLiveUsername("  @someone "))

    @Test
    fun pastedLiveUrl() =
        assertEquals("some.one", normalizeLiveUsername("https://www.tiktok.com/@some.one/live?lang=en"))

    @Test
    fun blank() = assertEquals("", normalizeLiveUsername("   "))
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.stillhere.app.live.LiveUsernameTest"`
Expected: compilation FAILS with `Unresolved reference: normalizeLiveUsername`.

- [ ] **Step 3: Extract the function**

`app/src/main/java/com/stillhere/app/live/LiveUsername.kt`:

```kotlin
package com.stillhere.app.live

/** Accept a bare handle, "@handle", or a pasted profile/live URL. */
fun normalizeLiveUsername(raw: String): String {
    var s = raw.trim()
    val marker = "tiktok.com/@"
    val at = s.indexOf(marker)
    if (at >= 0) s = s.substring(at + marker.length)
    return s.substringBefore('/').substringBefore('?').trim().trimStart('@').trim()
}
```

- [ ] **Step 4: Run the test to see it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.stillhere.app.live.LiveUsernameTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Add `Destination` and `Features`**

`app/src/main/java/com/stillhere/app/ui/Destination.kt`:

```kotlin
package com.stillhere.app.ui

/**
 * The three sections the web has, so both products share one mental model.
 * Which of them a build shows is its flavor's call — see `Features.destinations`.
 */
enum class Destination(val label: String) {
    Record("Record live"),
    Watch("Auto-record"),
    Save("Save post"),
}
```

Delete the `internal enum class Destination` block and its KDoc from `ui/AppRoot.kt`.

`app/src/main/java/com/stillhere/app/Features.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.ui.Destination

/** What a flavor offers. Decided once per flavor, in its `FlavorFeatures.kt`. */
data class Features(
    /** Tabs, in order. With a single tab no nav strip is drawn. */
    val destinations: List<Destination>,
    /** The line under "Sessions" saying where a login is kept. */
    val sessionsNote: String,
)
```

In `Flavor.kt`, add `val features: Features` as the first member of the interface.

`app/src/main/java/com/stillhere/app/FlavorFeatures.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.ui.Destination

val flavorFeatures = Features(
    destinations = listOf(Destination.Record, Destination.Watch, Destination.Save),
    sessionsNote = "Needed for private or age-restricted posts. Sign-in happens in a secure " +
        "web window and the session is stored on your server — credentials never " +
        "touch this app.",
)
```

In `FlavorModule.kt`, add `override val features: Features = flavorFeatures`.

- [ ] **Step 6: Move live state into `LiveViewModel`**

`app/src/main/java/com/stillhere/app/ui/record/LiveViewModel.kt`. Its members and bodies are moved verbatim from `AppViewModel`; only the owner and the `app.flavor` prefix change:

```kotlin
package com.stillhere.app.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import com.stillhere.app.live.LiveRecordingService
import com.stillhere.app.live.LiveState
import com.stillhere.app.live.normalizeLiveUsername
import com.stillhere.app.net.LiveStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs Record live: check whether a creator is live, then record to the gallery. */
class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<StillHereApp>()

    val liveState: StateFlow<LiveState> = app.flavor.liveController.state

    private val _liveUsername = MutableStateFlow("")
    val liveUsername: StateFlow<String> = _liveUsername.asStateFlow()

    private val _liveCheck = MutableStateFlow<LiveStatus?>(null)
    val liveCheck: StateFlow<LiveStatus?> = _liveCheck.asStateFlow()

    private val _liveChecking = MutableStateFlow(false)
    val liveChecking: StateFlow<Boolean> = _liveChecking.asStateFlow()

    fun onLiveUsernameChange(value: String) {
        _liveUsername.value = value
        _liveCheck.value = null
    }

    fun checkLive() {
        val username = normalizeLiveUsername(_liveUsername.value)
        if (username.isBlank()) return
        viewModelScope.launch {
            _liveChecking.value = true
            _liveCheck.value = runCatching { app.flavor.api.checkLive(username) }
                .getOrElse { LiveStatus(username = username, message = it.message ?: "Couldn't check status.") }
            _liveChecking.value = false
        }
    }

    fun startLiveRecording() {
        val username = normalizeLiveUsername(_liveUsername.value)
        if (username.isBlank()) return
        app.flavor.liveController.reset()
        LiveRecordingService.start(app, username)
    }

    fun stopLiveRecording() = LiveRecordingService.stop(app)

    fun dismissLive() = app.flavor.liveController.reset()
}
```

In `ui/record/RecordLiveScreen.kt`, change the signature to `fun RecordLiveScreen(viewModel: LiveViewModel = viewModel())`. Replace `import com.stillhere.app.ui.AppViewModel` with `import androidx.lifecycle.viewmodel.compose.viewModel`. `LiveViewModel` is in the same package, so it needs no import.

- [ ] **Step 7: Move auto-record state into `WatchViewModel`**

`app/src/main/java/com/stillhere/app/ui/watch/WatchViewModel.kt`:

```kotlin
package com.stillhere.app.ui.watch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import com.stillhere.app.live.normalizeLiveUsername
import com.stillhere.app.net.WatchJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs Auto-record: standing orders for creators who are not live yet. */
class WatchViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<StillHereApp>()

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
            if (app.flavor.settings.baseUrl().isBlank()) return@launch
            runCatching { app.flavor.api.listWatches() }
                .onSuccess { _watches.value = it }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't reach the register." }
        }
    }

    fun placeWatchOrder() {
        val username = normalizeLiveUsername(_watchUsername.value)
        if (username.isBlank()) {
            _watchNotice.value = "Enter a username or live URL."
            return
        }
        viewModelScope.launch {
            runCatching { app.flavor.api.createWatch(username, _watchDuration.value.toIntOrNull()) }
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
            runCatching { app.flavor.api.stopWatch(id) }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't stop that order." }
            refreshWatches()
        }
    }

    fun deleteWatch(id: String) {
        viewModelScope.launch {
            runCatching { app.flavor.api.deleteWatch(id) }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't discard that order." }
            refreshWatches()
        }
    }
}
```

In `ui/watch/AutoRecordScreen.kt`, change the signature to `fun AutoRecordScreen(viewModel: WatchViewModel = viewModel())`. Replace `import com.stillhere.app.ui.AppViewModel` with `import androidx.lifecycle.viewmodel.compose.viewModel`.

- [ ] **Step 8: Move the server settings out of `SessionsSheet`**

`app/src/main/java/com/stillhere/app/ui/sessions/ServerSettingsViewModel.kt`:

```kotlin
package com.stillhere.app.ui.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The backend URL and API key, as the Server section of the sheet edits them. */
class ServerSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings get() = getApplication<StillHereApp>().flavor.settings

    val baseUrl: StateFlow<String> =
        settings.baseUrlFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val apiKey: StateFlow<String> =
        settings.apiKeyFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun save(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            settings.setBaseUrl(baseUrl)
            settings.setApiKey(apiKey)
        }
    }
}
```

`app/src/main/java/com/stillhere/app/ui/sessions/ServerSettingsSection.kt`. The fields and button are moved verbatim from `SessionsSheet`:

```kotlin
package com.stillhere.app.ui.sessions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerField

/** The Server section of the sheet: where the backend is, and its key. */
@Composable
fun ServerSettingsSection(onDone: () -> Unit, viewModel: ServerSettingsViewModel = viewModel()) {
    val savedBaseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.apiKey.collectAsStateWithLifecycle()

    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }

    FiledHead("Server")
    Spacer(Modifier.height(16.dp))
    LedgerField(
        label = "Backend URL",
        value = baseUrl,
        onValueChange = { baseUrl = it },
        placeholder = "https://app.dioriza.com/stillhere",
    )
    Spacer(Modifier.height(16.dp))
    LedgerField(
        label = "API key — optional",
        value = apiKey,
        onValueChange = { apiKey = it },
        placeholder = "only if your server requires one",
    )
    Spacer(Modifier.height(20.dp))
    LedgerButton("Save", {
        viewModel.save(baseUrl, apiKey)
        onDone()
    })
}
```

`app/src/main/java/com/stillhere/app/ui/FlavorScreens.kt`:

```kotlin
package com.stillhere.app.ui

import androidx.compose.runtime.Composable
import com.stillhere.app.ui.record.RecordLiveScreen
import com.stillhere.app.ui.sessions.ServerSettingsSection
import com.stillhere.app.ui.watch.AutoRecordScreen

/** The tabs only this flavor has. Save post is shared and drawn by [AppRoot]. */
@Composable
fun FlavorDestination(destination: Destination) {
    when (destination) {
        Destination.Record -> RecordLiveScreen()
        Destination.Watch -> AutoRecordScreen()
        Destination.Save -> Unit
    }
}

/** This flavor's settings, at the top of the Sessions sheet. */
@Composable
fun FlavorSettingsSection(onDone: () -> Unit) = ServerSettingsSection(onDone)
```

- [ ] **Step 9: Slim `AppViewModel` down**

In `ui/AppViewModel.kt`:
- Delete `baseUrl`, `apiKey`, `saveSettings(...)`, the entire `// --- Live recording ---` section, `normalizeLiveUsername`, and the entire `// --- Auto-record ...` section.
- Delete the now-unused imports: `LiveRecordingService`, `LiveState`, `LiveStatus` and `WatchJob`.
- Add `import com.stillhere.app.Features`, then add these near the top of the class:

```kotlin
    val features: Features get() = app.flavor.features
```

- Replace the sessions section with:

```kotlin
    // --- Account sessions (for private / age-restricted downloads) ---

    private val _sessionsAvailable = MutableStateFlow(false)
    /** False in the server flavor until a backend URL is saved; always true locally. */
    val sessionsAvailable: StateFlow<Boolean> = _sessionsAvailable.asStateFlow()

    private val _tiktokConnected = MutableStateFlow(false)
    val tiktokConnected: StateFlow<Boolean> = _tiktokConnected.asStateFlow()

    private val _instagramConnected = MutableStateFlow(false)
    val instagramConnected: StateFlow<Boolean> = _instagramConnected.asStateFlow()

    /** Refresh per-platform login status from this flavor's session store. */
    fun refreshAuthStatus() {
        viewModelScope.launch {
            val sessions = app.flavor.sessionStore
            _sessionsAvailable.value = sessions.isAvailable()
            if (!_sessionsAvailable.value) return@launch
            _tiktokConnected.value = sessions.isConnected(Platform.TIKTOK)
            _instagramConnected.value = sessions.isConnected(Platform.INSTAGRAM)
        }
    }

    fun logout(platform: Platform) {
        viewModelScope.launch {
            runCatching { app.flavor.sessionStore.clear(platform) }
            refreshAuthStatus()
        }
    }
```

- Update the class KDoc to: `Backs the shared screens: history, the active download, the URL field, the clipboard banner and sessions. Flavor-only screens have their own view models.`

- [ ] **Step 10: Let `AppRoot` build its tabs from `Features`**

In `ui/AppRoot.kt`:
- Remove the imports of `RecordLiveScreen` and `AutoRecordScreen`.
- Replace the `AppRoot` function with:

```kotlin
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val destinations = viewModel.features.destinations
    var destination by rememberSaveable { mutableStateOf(Destination.Save) }
    var sessionsOpen by rememberSaveable { mutableStateOf(false) }

    val tiktokConnected by viewModel.tiktokConnected.collectAsStateWithLifecycle()
    val instagramConnected by viewModel.instagramConnected.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshAuthStatus() }

    Scaffold(
        containerColor = Ledger.Board,
        topBar = {
            Masthead(
                bothSessionsReady = tiktokConnected && instagramConnected,
                onOpenSessions = { sessionsOpen = true },
            )
        },
        bottomBar = {
            // A strip for choosing between tabs says nothing when there is one tab.
            if (destinations.size > 1) LedgerNavBar(destinations, destination) { destination = it }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (destination) {
                Destination.Save -> SavePostScreen(viewModel)
                else -> FlavorDestination(destination)
            }
        }
    }

    if (sessionsOpen) SessionsSheet(viewModel) { sessionsOpen = false }
}
```

- Change `LedgerNavBar`'s signature to `internal fun LedgerNavBar(entries: List<Destination>, current: Destination, onSelect: (Destination) -> Unit)`, and replace `Destination.entries.forEach` inside it with `entries.forEach`.

In `app/src/screenshotTest/java/com/stillhere/app/ui/AppShellPreviews.kt`, change `LedgerNavBar(Destination.Watch) {}` to `LedgerNavBar(Destination.entries, Destination.Watch) {}`.

- [ ] **Step 11: Let `SessionsSheet` take its settings from the flavor**

In `ui/sessions/SessionsSheet.kt`:
- Delete `savedBaseUrl`, `savedApiKey`, the `baseUrl`/`apiKey` `remember` states, and the `LaunchedEffect(savedBaseUrl)`.
- Add `val available by viewModel.sessionsAvailable.collectAsStateWithLifecycle()` and `LaunchedEffect(Unit) { viewModel.refreshAuthStatus() }`.
- Replace everything from `FiledHead("Server")` through the `LedgerButton("Save", ...)` with `FlavorSettingsSection(onDone = onDismiss)` (import `com.stillhere.app.ui.FlavorSettingsSection`).
- Change `if (savedBaseUrl.isNotBlank()) {` to `if (available) {`.
- Replace the hard-coded "Needed for private or age-restricted posts…" text with `viewModel.features.sessionsNote`.
- Change the KDoc's first line to `Sessions, and this flavor's settings above them.` and drop the sentence about the server.
- Remove the imports that are now unused: `LedgerField`, `mutableStateOf`, `remember`, `setValue`.

- [ ] **Step 12: Build, test, check screenshots**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:validateDebugScreenshotTest`
Expected: `BUILD SUCCESSFUL`. Screenshot validation passes with no diffs, because the pixels haven't changed.

Run: `grep -n "api\|settings\|live\|watch" app/src/main/java/com/stillhere/app/ui/AppViewModel.kt`
Expected: no output.

- [ ] **Step 13: Commit**

```bash
git add -A app/src
git commit -m "refactor: give live, auto-record and server settings their own view models"
```

---

### Task 5: Add the `server` and `local` flavors

Server-only code moves to `src/server`. The local flavor gets its identity, its session store, and a stand-in media source that Task 12 replaces.

**Files:**
- Modify: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Move (git mv) into `app/src/server/java/com/stillhere/app/`: `net/`, `data/SettingsRepo.kt`, `live/`, `ui/record/`, `ui/watch/`, `ui/sessions/ServerSettingsSection.kt`, `ui/sessions/ServerSettingsViewModel.kt`, `FlavorModule.kt`, `FlavorFeatures.kt`, `ui/FlavorScreens.kt`
- Move: `app/src/test/java/com/stillhere/app/net/` and `app/src/test/java/com/stillhere/app/live/` → `app/src/testServer/java/com/stillhere/app/`
- Move: `app/src/debug/screenshotTest/reference` → `app/src/serverDebug/screenshotTest/reference`
- Create: `app/src/server/AndroidManifest.xml`
- Create: `app/src/local/AndroidManifest.xml`, `app/src/local/res/values/strings.xml`, `app/src/local/res/drawable/ic_launcher_background.xml`
- Create: `app/src/local/java/com/stillhere/app/FlavorModule.kt`, `FlavorFeatures.kt`, `ui/FlavorScreens.kt`, `auth/LocalSessionStore.kt`, `local/SessionCookies.kt`
- Test: `app/src/testServer/java/com/stillhere/app/FlavorFeaturesTest.kt`, `app/src/testLocal/java/com/stillhere/app/FlavorFeaturesTest.kt`, `app/src/testLocal/java/com/stillhere/app/local/SessionCookiesTest.kt`

**Interfaces:**
- Consumes: `Flavor`, `Features`, `Destination`, `SessionStore`, `MediaSource`, `MediaException` (Tasks 3–4).
- Produces:
  - `SessionCookies.domain(platform: Platform): String?`
  - `SessionCookies.netscapeFile(platform: Platform, sessionId: String, expiresEpochSeconds: Long): String?`
  - `class LocalSessionStore(context: Context) : SessionStore`, with the extra `suspend fun sessionId(platform: Platform): String?`
  - local `FlavorModule.sessions: LocalSessionStore`

- [ ] **Step 1: Declare the flavors**

In `app/build.gradle.kts`, inside `android { }` after `defaultConfig { }`, add:

```kotlin
    flavorDimensions += "engine"
    productFlavors {
        // Today's app: the backend does the work. No suffix, so installs keep upgrading.
        create("server") {
            dimension = "engine"
        }
        // Does the work on the phone; installs beside the server app.
        create("local") {
            dimension = "engine"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
        }
    }
```

- [ ] **Step 2: Move the server-only code**

```bash
S=app/src/server/java/com/stillhere/app
M=app/src/main/java/com/stillhere/app
mkdir -p $S/data $S/ui/sessions app/src/testServer/java/com/stillhere/app app/src/serverDebug/screenshotTest
git mv $M/net $S/net
git mv $M/data/SettingsRepo.kt $S/data/SettingsRepo.kt
git mv $M/live $S/live
git mv $M/ui/record $S/ui/record
git mv $M/ui/watch $S/ui/watch
git mv $M/ui/sessions/ServerSettingsSection.kt $S/ui/sessions/
git mv $M/ui/sessions/ServerSettingsViewModel.kt $S/ui/sessions/
git mv $M/FlavorModule.kt $S/FlavorModule.kt
git mv $M/FlavorFeatures.kt $S/FlavorFeatures.kt
git mv $M/ui/FlavorScreens.kt $S/ui/FlavorScreens.kt
git mv app/src/test/java/com/stillhere/app/net app/src/testServer/java/com/stillhere/app/net
git mv app/src/test/java/com/stillhere/app/live app/src/testServer/java/com/stillhere/app/live
git mv app/src/debug/screenshotTest/reference app/src/serverDebug/screenshotTest/reference
```

- [ ] **Step 3: Split the manifest**

In `app/src/main/AndroidManifest.xml`, delete the `LiveRecordingService` `<service>` element and the comment above it.

Create `app/src/server/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application>
        <!-- Streams a TikTok live to the gallery for the whole broadcast. -->
        <service
            android:name=".live.LiveRecordingService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
    </application>

</manifest>
```

Create `app/src/local/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Cleartext exists for a backend on a private network. Local has none. -->
    <application
        android:usesCleartextTraffic="false"
        tools:replace="android:usesCleartextTraffic" />

</manifest>
```

- [ ] **Step 4: Give Local its name and icon**

`app/src/local/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Still Here Local</string>
</resources>
```

`app/src/local/res/drawable/ic_launcher_background.xml`. It has the same shape as main's, but uses a pale green paper so the two icons are easy to tell apart side by side:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Local's paper: the ledger's cream pushed toward Instagram's ink, so
         the two apps read as siblings but not twins on a home screen. -->
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108">
                <item android:offset="0" android:color="#FFEEF3EC" />
                <item android:offset="1" android:color="#FFDCE6DA" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
```

- [ ] **Step 5: Write the failing tests**

`app/src/testServer/java/com/stillhere/app/FlavorFeaturesTest.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.ui.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlavorFeaturesTest {

    @Test
    fun serverKeepsAllThreeTabs() =
        assertEquals(listOf(Destination.Record, Destination.Watch, Destination.Save), flavorFeatures.destinations)

    @Test
    fun serverSaysSessionsLiveOnTheServer() = assertTrue("your server" in flavorFeatures.sessionsNote)
}
```

`app/src/testLocal/java/com/stillhere/app/FlavorFeaturesTest.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.ui.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlavorFeaturesTest {

    @Test
    fun localHasOnlySavePost() = assertEquals(listOf(Destination.Save), flavorFeatures.destinations)

    @Test
    fun localSaysSessionsStayOnThePhone() = assertTrue("this phone" in flavorFeatures.sessionsNote)
}
```

`app/src/testLocal/java/com/stillhere/app/local/SessionCookiesTest.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCookiesTest {

    @Test
    fun writesOneTabSeparatedCookie() {
        val lines = SessionCookies.netscapeFile(Platform.INSTAGRAM, "abc123", 2_000_000_000)!!.trimEnd().lines()
        assertEquals("# Netscape HTTP Cookie File", lines[0])
        assertEquals(
            listOf(".instagram.com", "TRUE", "/", "TRUE", "2000000000", "sessionid", "abc123"),
            lines[1].split("\t"),
        )
    }

    @Test
    fun tiktokCookieCoversEverySubdomain() = assertEquals(".tiktok.com", SessionCookies.domain(Platform.TIKTOK))

    @Test
    fun youtubeHasNoSession() {
        assertNull(SessionCookies.domain(Platform.YOUTUBE))
        assertNull(SessionCookies.netscapeFile(Platform.YOUTUBE, "x", 1))
    }
}
```

- [ ] **Step 6: Run them to see them fail**

Run: `./gradlew :app:testLocalDebugUnitTest`
Expected: compilation FAILS. `src/local` has no `FlavorModule`, `flavorFeatures`, `FlavorDestination` or `SessionCookies` yet.

- [ ] **Step 7: Implement the local flavor's shell**

`app/src/local/java/com/stillhere/app/local/SessionCookies.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform

/** Cookie facts for the two platforms that keep a session. */
object SessionCookies {

    /** The cookie domain, leading dot included, or null for YouTube. */
    fun domain(platform: Platform): String? = when (platform) {
        Platform.TIKTOK -> ".tiktok.com"
        Platform.INSTAGRAM -> ".instagram.com"
        Platform.YOUTUBE -> null
    }

    /**
     * A Netscape cookies.txt holding one `sessionid`, as yt-dlp's `--cookies` reads it.
     * Fields are tab-separated: domain, include subdomains, path, secure, expiry, name, value.
     */
    fun netscapeFile(platform: Platform, sessionId: String, expiresEpochSeconds: Long): String? {
        val domain = domain(platform) ?: return null
        val cookie = listOf(domain, "TRUE", "/", "TRUE", expiresEpochSeconds.toString(), "sessionid", sessionId)
        return "# Netscape HTTP Cookie File\n" + cookie.joinToString("\t") + "\n"
    }
}
```

`app/src/local/java/com/stillhere/app/auth/LocalSessionStore.kt`:

```kotlin
package com.stillhere.app.auth

import android.content.Context
import android.webkit.CookieManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stillhere.app.domain.Platform
import com.stillhere.app.local.SessionCookies
import kotlinx.coroutines.flow.first

private val Context.sessionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sessions")

/**
 * Keeps each platform's `sessionid` on this phone only, in app-private storage.
 * It is sent nowhere but the platform it belongs to.
 */
class LocalSessionStore(private val context: Context) : SessionStore {

    override suspend fun isAvailable(): Boolean = true

    override suspend fun isConnected(platform: Platform): Boolean = sessionId(platform) != null

    suspend fun sessionId(platform: Platform): String? {
        val key = keyFor(platform) ?: return null
        return context.sessionsDataStore.data.first()[key]?.takeIf { it.isNotBlank() }
    }

    override suspend fun save(platform: Platform, sessionId: String) {
        val key = keyFor(platform) ?: return
        context.sessionsDataStore.edit { it[key] = sessionId }
    }

    override suspend fun clear(platform: Platform) {
        val key = keyFor(platform) ?: return
        context.sessionsDataStore.edit { it.remove(key) }
        // Expire the WebView's copy too, so the next sign-in starts at a login page.
        val domain = SessionCookies.domain(platform) ?: return
        CookieManager.getInstance().apply {
            setCookie("https://www$domain", "sessionid=; Domain=$domain; Path=/; Max-Age=0")
            flush()
        }
    }

    private fun keyFor(platform: Platform) = when (platform) {
        Platform.TIKTOK -> KEY_TIKTOK
        Platform.INSTAGRAM -> KEY_INSTAGRAM
        Platform.YOUTUBE -> null // fetched anonymously
    }

    private companion object {
        val KEY_TIKTOK = stringPreferencesKey("tiktok_sessionid")
        val KEY_INSTAGRAM = stringPreferencesKey("instagram_sessionid")
    }
}
```

`app/src/local/java/com/stillhere/app/FlavorFeatures.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.ui.Destination

val flavorFeatures = Features(
    // Phase 1: saving only. Record live returns in phase 2; auto-record never does.
    destinations = listOf(Destination.Save),
    sessionsNote = "Needed for private or age-restricted posts, and for Instagram photo " +
        "posts. Sign-in happens in a secure web window and the session stays on this " +
        "phone — credentials never touch this app.",
)
```

`app/src/local/java/com/stillhere/app/ui/FlavorScreens.kt`:

```kotlin
package com.stillhere.app.ui

import androidx.compose.runtime.Composable

/** Local has no tabs of its own in phase 1; Save post is shared. */
@Composable
fun FlavorDestination(destination: Destination) = Unit

/** Local's settings, at the top of the Sessions sheet. Filled in by the yt-dlp task. */
@Composable
fun FlavorSettingsSection(onDone: () -> Unit) = Unit
```

`app/src/local/java/com/stillhere/app/FlavorModule.kt`:

```kotlin
package com.stillhere.app

import com.stillhere.app.auth.LocalSessionStore
import com.stillhere.app.auth.SessionStore
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import com.stillhere.app.download.MediaSource
import java.io.InputStream

/** The local flavor: everything happens on this phone. */
class FlavorModule(app: StillHereApp) : Flavor {
    val sessions: LocalSessionStore by lazy { LocalSessionStore(app) }

    override val features: Features = flavorFeatures
    override val sessionStore: SessionStore get() = sessions

    // Stand-in until LocalMediaSource is wired (Task 12).
    override val mediaSource: MediaSource = object : MediaSource {
        override suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit) {
            throw MediaException("Saving isn't wired up in this build yet.")
        }
    }
}
```

- [ ] **Step 8: Build both flavors and run every test**

Run: `./gradlew :app:assembleServerDebug :app:assembleLocalDebug :app:testServerDebugUnitTest :app:testLocalDebugUnitTest :app:validateServerDebugScreenshotTest`
Expected: `BUILD SUCCESSFUL`. If screenshot validation reports missing references, its message names the directory it expects. Move `app/src/serverDebug/screenshotTest/reference` there with `git mv` and run it again.

Run: `grep -rln "com.stillhere.app.net\|SettingsRepo\|LiveController\|WatchJob" app/src/main app/src/local`
Expected: no output. This is the check that the local app contains no backend code.

- [ ] **Step 9: Commit**

```bash
git add -A app/src app/build.gradle.kts
git commit -m "feat: split into server and local flavors; local keeps sessions on the phone"
```

---

### Task 6: Local routing and yt-dlp arguments

**Files:**
- Create: `app/src/local/java/com/stillhere/app/local/LocalRoute.kt`
- Create: `app/src/local/java/com/stillhere/app/local/YtDlpArgs.kt`
- Test: `app/src/testLocal/java/com/stillhere/app/local/LocalRouteTest.kt`, `YtDlpArgsTest.kt`

**Interfaces:**
- Consumes: `UrlRouter.Routed`, `Platform`.
- Produces:
  - `enum class Engine { YT_DLP, TIKWM, INSTAGRAM }`
  - `LocalRoute.plan(routed: UrlRouter.Routed): List<Engine>`
  - `LocalRoute.isTikTokPhotoPost(url: String): Boolean`
  - `data class YtDlpOption(val name: String, val value: String? = null)`
  - `YtDlpArgs.build(platform: Platform, outDir: File, cookieFile: File?): List<YtDlpOption>`

- [ ] **Step 1: Write the failing tests**

`app/src/testLocal/java/com/stillhere/app/local/LocalRouteTest.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.UrlRouter
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalRouteTest {

    private fun plan(url: String) = LocalRoute.plan(UrlRouter.route(url)!!)

    @Test
    fun tiktokVideoTriesYtDlpThenTikwm() =
        assertEquals(listOf(Engine.YT_DLP, Engine.TIKWM), plan("https://www.tiktok.com/@someone/video/7300000000000000000"))

    @Test
    fun tiktokPhotoPostGoesStraightToTikwm() =
        assertEquals(listOf(Engine.TIKWM), plan("https://www.tiktok.com/@someone/photo/7300000000000000000?lang=en"))

    @Test
    fun aShortLinkCannotBeToldApartSoItTakesTheLongWay() =
        assertEquals(listOf(Engine.YT_DLP, Engine.TIKWM), plan("https://vm.tiktok.com/ZMabc123/"))

    @Test
    fun aHandleContainingPhotoIsNotAPhotoPost() =
        assertEquals(listOf(Engine.YT_DLP, Engine.TIKWM), plan("https://www.tiktok.com/@photo/video/1"))

    @Test
    fun instagramTriesYtDlpThenTheExtractor() =
        assertEquals(listOf(Engine.YT_DLP, Engine.INSTAGRAM), plan("https://www.instagram.com/p/Cabc123XYZ/"))

    @Test
    fun youtubeIsYtDlpOnly() =
        assertEquals(listOf(Engine.YT_DLP), plan("https://youtube.com/shorts/abcDEF12345"))
}
```

`app/src/testLocal/java/com/stillhere/app/local/YtDlpArgsTest.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class YtDlpArgsTest {

    private val dir = File("jobs/1/yt_dlp")

    private fun flat(platform: Platform, cookies: File? = null) =
        YtDlpArgs.build(platform, dir, cookies).flatMap { listOfNotNull(it.name, it.value) }

    @Test
    fun tiktokIsTheServersCommandMinusImpersonate() = assertEquals(
        listOf(
            "--no-playlist",
            "--check-formats",
            "--add-header", "Referer: https://www.tiktok.com/",
            "--paths", dir.absolutePath,
            "--output", "%(title).80s-%(id)s.%(ext)s",
        ),
        flat(Platform.TIKTOK),
    )

    @Test
    fun neverImpersonates() = Platform.entries.forEach { assertFalse(flat(it).contains("--impersonate")) }

    @Test
    fun instagramKeepsCarousels() = assertFalse(flat(Platform.INSTAGRAM).contains("--no-playlist"))

    @Test
    fun cookiesOnlyWhenGiven() {
        val cookies = File("jobs/1/cookies.txt")
        val args = flat(Platform.INSTAGRAM, cookies)
        assertEquals(cookies.absolutePath, args[args.indexOf("--cookies") + 1])
        assertFalse(flat(Platform.INSTAGRAM).contains("--cookies"))
    }

    @Test
    fun youtubeAsksForAnMp4AndSendsNoReferer() {
        val args = flat(Platform.YOUTUBE)
        assertEquals("bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b", args[args.indexOf("-f") + 1])
        assertEquals("mp4", args[args.indexOf("--merge-output-format") + 1])
        assertFalse(args.contains("--add-header"))
    }
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.*"`
Expected: compilation FAILS with `Unresolved reference: LocalRoute` and `Unresolved reference: YtDlpArgs`.

- [ ] **Step 3: Implement**

`app/src/local/java/com/stillhere/app/local/LocalRoute.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import com.stillhere.app.domain.UrlRouter
import java.net.URI

/** The ways Local can turn a link into files. */
enum class Engine { YT_DLP, TIKWM, INSTAGRAM }

/** Which engines to try for a link, in order. The first to produce media wins. */
object LocalRoute {

    fun plan(routed: UrlRouter.Routed): List<Engine> = when (routed.platform) {
        // yt-dlp cannot read photo posts, so tikwm is the only way in. A short
        // vm.tiktok.com link hides which kind it is and takes the long way.
        Platform.TIKTOK ->
            if (isTikTokPhotoPost(routed.url)) listOf(Engine.TIKWM) else listOf(Engine.YT_DLP, Engine.TIKWM)
        // yt-dlp fails fast on photo posts, so trying it first costs little.
        Platform.INSTAGRAM -> listOf(Engine.YT_DLP, Engine.INSTAGRAM)
        Platform.YOUTUBE -> listOf(Engine.YT_DLP)
    }

    fun isTikTokPhotoPost(url: String): Boolean {
        val path = runCatching { URI(url).path }.getOrNull().orEmpty()
        return path.split('/').contains("photo")
    }
}
```

`app/src/local/java/com/stillhere/app/local/YtDlpArgs.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import java.io.File

/** One yt-dlp option; a bare flag has no value. */
data class YtDlpOption(val name: String, val value: String? = null)

/** The server's yt-dlp command line, minus the URL and minus --impersonate. */
object YtDlpArgs {

    const val OUTPUT_TEMPLATE = "%(title).80s-%(id)s.%(ext)s"

    fun build(platform: Platform, outDir: File, cookieFile: File?): List<YtDlpOption> = buildList {
        // The server passes --no-playlist for TikTok and YouTube but not for
        // Instagram, where a carousel is a playlist and every item is wanted.
        if (platform != Platform.INSTAGRAM) add(YtDlpOption("--no-playlist"))
        add(YtDlpOption("--check-formats"))
        // No --impersonate: it needs curl_cffi, which has no Android build.
        referer(platform)?.let { add(YtDlpOption("--add-header", "Referer: $it")) }
        add(YtDlpOption("--paths", outDir.absolutePath))
        add(YtDlpOption("--output", OUTPUT_TEMPLATE))
        if (cookieFile != null) add(YtDlpOption("--cookies", cookieFile.absolutePath))
        if (platform == Platform.YOUTUBE) {
            // An MP4 the gallery can play; the bundled ffmpeg merges the two streams.
            add(YtDlpOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b"))
            add(YtDlpOption("--merge-output-format", "mp4"))
        }
    }

    private fun referer(platform: Platform): String? = when (platform) {
        Platform.TIKTOK -> "https://www.tiktok.com/"
        Platform.INSTAGRAM -> "https://www.instagram.com/"
        Platform.YOUTUBE -> null
    }
}
```

- [ ] **Step 4: Run them to see them pass**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/local app/src/testLocal
git commit -m "feat: route local links to engines and build yt-dlp's arguments"
```

---

### Task 7: Friendly errors

**Files:**
- Create: `app/src/local/java/com/stillhere/app/local/FriendlyError.kt`
- Test: `app/src/testLocal/java/com/stillhere/app/local/FriendlyErrorTest.kt`

**Interfaces:**
- Produces:
  - `class EngineFailure(val raw: String) : Exception(raw)`
  - `FriendlyError.from(raw: String, platform: Platform): String`
  - `FriendlyError.saysNoVideo(raw: String): Boolean`
  - `FriendlyError.UPDATE_HINT`

- [ ] **Step 1: Write the failing test**

`app/src/testLocal/java/com/stillhere/app/local/FriendlyErrorTest.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendlyErrorTest {

    private fun tiktok(raw: String) = FriendlyError.from(raw, Platform.TIKTOK)
    private fun instagram(raw: String) = FriendlyError.from(raw, Platform.INSTAGRAM)

    @Test
    fun deadTikTokPostIsGoneNotABugReport() = assertEquals(
        "This post is gone — deleted, private, or region-locked.",
        tiktok(
            "ERROR: [TikTok] 7300: Unexpected response from webpage request; please report this issue on " +
                " https://github.com/yt-dlp/yt-dlp/issues?q= , filling out the appropriate issue template.",
        ),
    )

    @Test
    fun instagramLoginWallAsksForASignIn() = assertEquals(
        "Sign in to Instagram to save this.",
        instagram(
            "ERROR: [Instagram] Cabc: Requested content is not available, rate-limit reached or login required. " +
                "Use --cookies, --cookies-from-browser, --username and --password",
        ),
    )

    @Test
    fun youtubeLoginWallSaysThereIsNoLoginToOffer() = assertEquals(
        "YouTube won't serve this without a login, which Still Here doesn't keep.",
        FriendlyError.from("ERROR: [youtube] abc: Sign in to confirm your age.", Platform.YOUTUBE),
    )

    @Test
    fun noNetwork() = assertEquals(
        "No connection. Check your network and try again.",
        tiktok("ERROR: Unable to download webpage: <urlopen error [Errno 7] No address associated with hostname>"),
    )

    @Test
    fun rateLimited() = assertEquals(
        "TikTok is rate-limiting. Try again in a few minutes.",
        tiktok("ERROR: Unable to download webpage: HTTP Error 429: Too Many Requests"),
    )

    @Test
    fun unsupportedLink() = assertEquals(
        "That link isn't a post Still Here can save.",
        tiktok("ERROR: Unsupported URL: https://www.tiktok.com/@someone"),
    )

    @Test
    fun brokenExtractorSuggestsAnUpdate() = assertEquals(
        "TikTok did not return this post. ${FriendlyError.UPDATE_HINT}",
        tiktok("ERROR: [TikTok] 1: Unable to extract universal data for rehydration; please report this issue on https://github.com/yt-dlp/yt-dlp/issues"),
    )

    @Test
    fun anythingElseIsItsLastLineWithoutThePrefix() =
        assertEquals("something odd happened", tiktok("WARNING: foo\nERROR: something odd happened\n"))

    @Test
    fun silence() = assertEquals("The download failed without saying why.", tiktok("  "))

    @Test
    fun recognisesAPostWithNoVideo() {
        assertTrue(FriendlyError.saysNoVideo("ERROR: [Instagram] Cabc: There is no video in this post"))
        assertFalse(FriendlyError.saysNoVideo("ERROR: Unsupported URL: x"))
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.FriendlyErrorTest"`
Expected: compilation FAILS with `Unresolved reference: FriendlyError`.

- [ ] **Step 3: Implement**

`app/src/local/java/com/stillhere/app/local/FriendlyError.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform

/** An engine gave up. [raw] is what it said, usually yt-dlp's stderr. */
class EngineFailure(val raw: String) : Exception(raw)

/**
 * Turns an engine's raw failure into a sentence for the person who shared the
 * link. Ported from the server's `friendly_download_error`, then widened for
 * failures only a phone meets (no network, no session).
 */
object FriendlyError {

    const val UPDATE_HINT = "Updating yt-dlp in Settings often fixes this."

    private val NETWORK = listOf(
        "unable to resolve host", "failed to resolve", "no address associated with hostname",
        "name or service not known", "network is unreachable", "timed out",
    )
    // Instagram's "rate-limit reached or login required" is almost always the
    // login, so login is checked before the rate limit.
    private val LOGIN = listOf("login required", "log in", "sign in to confirm", "private video", "this account is private")
    private val RATE_LIMIT = listOf("http error 429", "too many requests")
    private val GONE = listOf(
        "unexpected response from webpage request", "video currently unavailable", "video unavailable",
        "this post is unavailable", "http error 404", "has been removed", "no longer available",
    )
    // An error that asks the reader to open an issue is aimed at a yt-dlp
    // maintainer, not at someone who pasted a link.
    private val BROKEN_EXTRACTOR = listOf("github.com/yt-dlp", "please report this issue", "yt-dlp -u", "unable to extract")

    fun from(raw: String, platform: Platform): String {
        val text = raw.trim()
        val lower = text.lowercase()
        val name = platform.label
        return when {
            text.isEmpty() -> "The download failed without saying why."
            NETWORK.any { it in lower } -> "No connection. Check your network and try again."
            LOGIN.any { it in lower } ->
                if (platform == Platform.YOUTUBE) "YouTube won't serve this without a login, which Still Here doesn't keep."
                else "Sign in to $name to save this."
            RATE_LIMIT.any { it in lower } -> "$name is rate-limiting. Try again in a few minutes."
            GONE.any { it in lower } -> "This post is gone — deleted, private, or region-locked."
            "unsupported url" in lower -> "That link isn't a post Still Here can save."
            BROKEN_EXTRACTOR.any { it in lower } -> "$name did not return this post. $UPDATE_HINT"
            else -> lastLine(text)
        }
    }

    /** yt-dlp's answer to an image-only post: true, but the next engine explains it better. */
    fun saysNoVideo(raw: String): Boolean = "no video in this post" in raw.lowercase()

    private fun lastLine(text: String): String =
        text.lines().map { it.trim() }.last { it.isNotEmpty() }.removePrefix("ERROR:").trim()
}
```

- [ ] **Step 4: Run it to see it pass**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.FriendlyErrorTest"`
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/local app/src/testLocal
git commit -m "feat: say what went wrong in the register's words, not yt-dlp's"
```

---

### Task 8: Instagram shortcodes and the media parser

**Files:**
- Create: `app/src/local/java/com/stillhere/app/local/RemoteMedia.kt`
- Create: `app/src/local/java/com/stillhere/app/local/InstagramShortcode.kt`
- Create: `app/src/local/java/com/stillhere/app/local/InstagramMediaParser.kt`
- Test: `app/src/testLocal/java/com/stillhere/app/local/InstagramShortcodeTest.kt`, `InstagramMediaParserTest.kt`

**Interfaces:**
- Produces:
  - `data class RemoteMedia(val url: String, val stem: String, val defaultExtension: String)`
  - `InstagramShortcode.fromUrl(url: String): String?`
  - `InstagramShortcode.toMediaId(shortcode: String): String`
  - `InstagramMediaParser.parse(body: String, shortcode: String): List<RemoteMedia>`

- [ ] **Step 1: Write the failing tests**

`app/src/testLocal/java/com/stillhere/app/local/InstagramShortcodeTest.kt`:

```kotlin
package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramShortcodeTest {

    @Test
    fun readsPostReelAndTvLinks() {
        assertEquals("DAbc", InstagramShortcode.fromUrl("https://www.instagram.com/p/DAbc/"))
        assertEquals("Cabc_12-3", InstagramShortcode.fromUrl("https://www.instagram.com/reel/Cabc_12-3/?igsh=xyz"))
        assertEquals("Bxyz", InstagramShortcode.fromUrl("https://instagram.com/tv/Bxyz"))
    }

    @Test
    fun aProfileHasNoShortcode() = assertNull(InstagramShortcode.fromUrl("https://www.instagram.com/someone/"))

    @Test
    fun decodesBase64Digits() {
        assertEquals("0", InstagramShortcode.toMediaId("A"))
        assertEquals("1", InstagramShortcode.toMediaId("B"))
        assertEquals("64", InstagramShortcode.toMediaId("BA"))
        assertEquals("4095", InstagramShortcode.toMediaId("__"))
    }

    @Test
    fun dropsAPrivatePostsSuffix() = assertEquals("64", InstagramShortcode.toMediaId("BA" + "x".repeat(28)))

    @Test
    fun handlesIdsBeyondALong() =
        assertEquals("73786976294838206463", InstagramShortcode.toMediaId("_".repeat(11)))
}
```

`app/src/testLocal/java/com/stillhere/app/local/InstagramMediaParserTest.kt`:

```kotlin
package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramMediaParserTest {

    @Test
    fun takesTheLargestOfEachCarouselItemAndPrefersVideo() {
        val body = """
            {"items":[{"code":"Cabc","media_type":8,"carousel_media":[
              {"media_type":1,"image_versions2":{"candidates":[
                {"width":320,"height":400,"url":"https://cdn.example/small1.jpg"},
                {"width":1080,"height":1350,"url":"https://cdn.example/big1.jpg"}]}},
              {"media_type":2,
               "image_versions2":{"candidates":[{"width":1080,"height":1920,"url":"https://cdn.example/thumb2.jpg"}]},
               "video_versions":[
                {"width":480,"height":854,"url":"https://cdn.example/low2.mp4"},
                {"width":720,"height":1280,"url":"https://cdn.example/high2.mp4"}]}
            ]}],"num_results":1,"status":"ok"}
        """.trimIndent()

        assertEquals(
            listOf(
                RemoteMedia("https://cdn.example/big1.jpg", "Cabc-01", ".jpg"),
                RemoteMedia("https://cdn.example/high2.mp4", "Cabc-02", ".mp4"),
            ),
            InstagramMediaParser.parse(body, "Cabc"),
        )
    }

    @Test
    fun aSingleImagePostIsOneFile() {
        val body = """
            {"items":[{"media_type":1,"image_versions2":{"candidates":[
              {"width":1080,"height":1080,"url":"https://cdn.example/one.jpg"}]}}],"status":"ok"}
        """.trimIndent()
        assertEquals(listOf(RemoteMedia("https://cdn.example/one.jpg", "Dxyz-01", ".jpg")), InstagramMediaParser.parse(body, "Dxyz"))
    }

    @Test
    fun nothingUsable() {
        assertTrue(InstagramMediaParser.parse("""{"items":[],"status":"ok"}""", "x").isEmpty())
        assertTrue(InstagramMediaParser.parse("""<html>login</html>""", "x").isEmpty())
    }
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.Instagram*"`
Expected: compilation FAILS with `Unresolved reference: InstagramShortcode`.

- [ ] **Step 3: Implement**

`app/src/local/java/com/stillhere/app/local/RemoteMedia.kt`:

```kotlin
package com.stillhere.app.local

/**
 * One file a fallback engine should fetch. The file is written as [stem] plus an
 * extension read from the response, or [defaultExtension] when it says nothing.
 */
data class RemoteMedia(val url: String, val stem: String, val defaultExtension: String)
```

`app/src/local/java/com/stillhere/app/local/InstagramShortcode.kt`:

```kotlin
package com.stillhere.app.local

import java.math.BigInteger
import java.net.URI

/** Instagram's shortcode is its media id written in URL-safe base64. */
object InstagramShortcode {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val BASE = BigInteger.valueOf(64)
    private val IN_PATH = Regex("""/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)""")

    // A private post's code carries a 28-character suffix after the public one,
    // which gallery-dl drops before decoding.
    private const val PRIVATE_SUFFIX = 28

    /** The shortcode in a post, reel or TV link, or null for anything else. */
    fun fromUrl(url: String): String? {
        val path = runCatching { URI(url).path }.getOrNull() ?: return null
        return IN_PATH.find(path)?.groupValues?.get(1)
    }

    /** The numeric media id `api/v1/media/{id}/info/` wants. */
    fun toMediaId(shortcode: String): String {
        val code = if (shortcode.length > PRIVATE_SUFFIX) shortcode.dropLast(PRIVATE_SUFFIX) else shortcode
        return code.fold(BigInteger.ZERO) { id, c ->
            val digit = ALPHABET.indexOf(c)
            require(digit >= 0) { "Not a shortcode character: $c" }
            id.multiply(BASE).add(BigInteger.valueOf(digit.toLong()))
        }.toString()
    }
}
```

`app/src/local/java/com/stillhere/app/local/InstagramMediaParser.kt`:

```kotlin
package com.stillhere.app.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Reads `api/v1/media/{id}/info/` into the files a post is made of, in order. */
object InstagramMediaParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String, shortcode: String): List<RemoteMedia> {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return emptyList()
        val item = (root["items"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return emptyList()
        val parts = (item["carousel_media"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: listOf(item)
        return parts.mapIndexedNotNull { index, part ->
            val stem = "$shortcode-${(index + 1).toString().padStart(2, '0')}"
            // A video item also carries images (its thumbnail); the video is the post.
            largest(part["video_versions"] as? JsonArray)?.let { RemoteMedia(it, stem, ".mp4") }
                ?: largest((part["image_versions2"] as? JsonObject)?.get("candidates") as? JsonArray)
                    ?.let { RemoteMedia(it, stem, ".jpg") }
        }
    }

    private fun largest(versions: JsonArray?): String? =
        versions?.mapNotNull { it as? JsonObject }
            ?.maxByOrNull { area(it) }
            ?.get("url")?.jsonPrimitive?.contentOrNull

    private fun area(version: JsonObject): Long {
        val width = version["width"]?.jsonPrimitive?.longOrNull ?: 0
        val height = version["height"]?.jsonPrimitive?.longOrNull ?: 0
        return width * height
    }
}
```

- [ ] **Step 4: Run them to see them pass**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.Instagram*"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/local app/src/testLocal
git commit -m "feat: read Instagram photo posts and carousels from the media API"
```

---

### Task 9: tikwm parser, URL guard, and remote downloader

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (add MockWebServer for `testLocal`)
- Create: `app/src/local/java/com/stillhere/app/local/TikwmParser.kt`, `PublicUrl.kt`, `MediaExtension.kt`, `RemoteDownloader.kt`
- Test: `app/src/testLocal/java/com/stillhere/app/local/TikwmParserTest.kt`, `PublicUrlTest.kt`, `MediaExtensionTest.kt`, `RemoteDownloaderTest.kt`

**Interfaces:**
- Consumes: `RemoteMedia` (Task 8).
- Produces:
  - `TikwmParser.parse(body: String): List<RemoteMedia>`
  - `PublicUrl.ensurePublic(url: String, resolve: (String) -> List<InetAddress> = ...)`, which throws `IllegalArgumentException`
  - `PublicUrl.isPublic(address: InetAddress): Boolean`
  - `MediaExtension.pick(contentType: String?, url: String, default: String): String`
  - `class RemoteDownloader(client: OkHttpClient, guard: (String) -> Unit = { PublicUrl.ensurePublic(it) }, maxBytes: Long = MAX_MEDIA_BYTES)` with `fun download(media: RemoteMedia, dir: File, referer: String): File`, which throws `IOException`
  - `RemoteDownloader.USER_AGENT`, `RemoteDownloader.MAX_MEDIA_BYTES`

- [ ] **Step 1: Add MockWebServer**

In `gradle/libs.versions.toml` under `[libraries]`:

```toml
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
```

In `app/build.gradle.kts` `dependencies { }`:

```kotlin
    "testLocalImplementation"(libs.okhttp.mockwebserver)
```

- [ ] **Step 2: Write the failing tests**

`app/src/testLocal/java/com/stillhere/app/local/TikwmParserTest.kt`:

```kotlin
package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TikwmParserTest {

    @Test
    fun aPhotoPostIsItsImagesInOrder() = assertEquals(
        listOf(
            RemoteMedia("https://p16.example/1.jpeg", "7301-image-001", ".jpg"),
            RemoteMedia("https://p16.example/2.jpeg", "7301-image-002", ".jpg"),
        ),
        TikwmParser.parse(
            """{"code":0,"msg":"success","data":{"id":"7301","images":["https://p16.example/1.jpeg","https://p16.example/2.jpeg"],
               "play":"https://v16.example/audio.mp3"}}""",
        ),
    )

    @Test
    fun aVideoPrefersTheCleanCopy() = assertEquals(
        listOf(RemoteMedia("https://v16.example/clean.mp4", "7302", ".mp4")),
        TikwmParser.parse("""{"code":0,"data":{"id":"7302","play":"https://v16.example/clean.mp4","wmplay":"https://v16.example/wm.mp4"}}"""),
    )

    @Test
    fun fallsBackToTheWatermarkedCopy() = assertEquals(
        listOf(RemoteMedia("https://v16.example/wm.mp4", "7303", ".mp4")),
        TikwmParser.parse("""{"code":0,"data":{"id":"7303","play":"","wmplay":"https://v16.example/wm.mp4"}}"""),
    )

    @Test
    fun resolvesRelativePathsAgainstTikwm() = assertEquals(
        "https://www.tikwm.com/video/media/play/7304.mp4",
        TikwmParser.parse("""{"code":0,"data":{"id":"7304","play":"/video/media/play/7304.mp4"}}""").single().url,
    )

    @Test
    fun aRefusalIsNothing() {
        assertTrue(TikwmParser.parse("""{"code":-1,"msg":"Url parsing is failed! Please check url."}""").isEmpty())
        assertTrue(TikwmParser.parse("not json").isEmpty())
    }
}
```

`app/src/testLocal/java/com/stillhere/app/local/PublicUrlTest.kt`:

```kotlin
package com.stillhere.app.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class PublicUrlTest {

    private fun ip(literal: String) = InetAddress.getByName(literal)

    @Test
    fun privateLoopbackAndMetadataAddressesAreNotPublic() {
        listOf(
            "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254",
            "100.64.0.1", "0.0.0.0", "::1", "fd00::1", "::ffff:127.0.0.1",
        ).forEach { assertFalse(it, PublicUrl.isPublic(ip(it))) }
    }

    @Test
    fun ordinaryAddressesArePublic() {
        listOf("8.8.8.8", "2606:4700::1111").forEach { assertTrue(it, PublicUrl.isPublic(ip(it))) }
    }

    @Test
    fun rejectsOtherSchemes() {
        assertThrows(IllegalArgumentException::class.java) { PublicUrl.ensurePublic("file:///etc/hosts") }
        assertThrows(IllegalArgumentException::class.java) { PublicUrl.ensurePublic("ftp://cdn.example/a.jpg") }
    }

    @Test
    fun rejectsAHostThatResolvesInside() {
        assertThrows(IllegalArgumentException::class.java) {
            PublicUrl.ensurePublic("https://cdn.example/a.jpg") { listOf(ip("8.8.8.8"), ip("10.0.0.5")) }
        }
    }

    @Test
    fun acceptsAHostThatResolvesOutside() =
        PublicUrl.ensurePublic("https://cdn.example/a.jpg") { listOf(ip("8.8.8.8")) }
}
```

`app/src/testLocal/java/com/stillhere/app/local/MediaExtensionTest.kt`:

```kotlin
package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaExtensionTest {

    @Test
    fun contentTypeWins() = assertEquals(".webp", MediaExtension.pick("image/webp; charset=binary", "https://x/a.jpg", ".jpg"))

    @Test
    fun thenTheUrlsSuffix() = assertEquals(".png", MediaExtension.pick("application/octet-stream", "https://x/a.PNG?sig=1", ".jpg"))

    @Test
    fun jpegIsSpelledJpg() = assertEquals(".jpg", MediaExtension.pick(null, "https://x/a.jpeg", ".mp4"))

    @Test
    fun otherwiseTheDefault() = assertEquals(".mp4", MediaExtension.pick(null, "https://x/stream", ".mp4"))
}
```

`app/src/testLocal/java/com/stillhere/app/local/RemoteDownloaderTest.kt`:

```kotlin
package com.stillhere.app.local

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class RemoteDownloaderTest {

    @get:Rule val temp = TemporaryFolder()
    private val server = MockWebServer()

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    // MockWebServer lives on 127.0.0.1, which the real guard rightly refuses.
    private fun downloader(maxBytes: Long = 1024) = RemoteDownloader(OkHttpClient(), guard = {}, maxBytes = maxBytes)

    private fun media() = RemoteMedia(server.url("/m/1").toString(), "post-01", ".jpg")

    @Test
    fun namesTheFileFromItsContentTypeAndSendsTheReferer() {
        server.enqueue(MockResponse().setHeader("Content-Type", "video/mp4").setBody("abc"))
        val file = downloader().download(media(), temp.root, "https://www.tiktok.com/")
        assertEquals("post-01.mp4", file.name)
        assertEquals("abc", file.readText())
        assertEquals("https://www.tiktok.com/", server.takeRequest().getHeader("Referer"))
    }

    @Test
    fun stopsAtTheCeilingAndLeavesNothingBehind() {
        server.enqueue(MockResponse().setBody(Buffer().write(ByteArray(2048))))
        assertThrows(IOException::class.java) { downloader(maxBytes = 1024).download(media(), temp.root, "r") }
        assertFalse(temp.root.listFiles()!!.any())
    }

    @Test
    fun anErrorStatusIsAFailure() {
        server.enqueue(MockResponse().setResponseCode(403))
        assertThrows(IOException::class.java) { downloader().download(media(), temp.root, "r") }
    }

    @Test
    fun theGuardRunsBeforeAnyRequest() {
        val guarded = RemoteDownloader(OkHttpClient(), guard = { throw IllegalArgumentException("inside") })
        assertThrows(IllegalArgumentException::class.java) { guarded.download(media(), temp.root, "r") }
        assertEquals(0, server.requestCount)
    }
}
```

- [ ] **Step 3: Run them to see them fail**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.*"`
Expected: compilation FAILS with unresolved `TikwmParser`, `PublicUrl`, `MediaExtension` and `RemoteDownloader`.

- [ ] **Step 4: Implement**

`app/src/local/java/com/stillhere/app/local/TikwmParser.kt`:

```kotlin
package com.stillhere.app.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Reads tikwm's `/api/?url=` answer: a photo post's images, or else the video.
 * Ported from the server's `_download_with_metadata_fallback`, except that the
 * clean `play` copy is preferred to the watermarked `wmplay` one.
 */
object TikwmParser {

    private const val ORIGIN = "https://www.tikwm.com"
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): List<RemoteMedia> {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return emptyList()
        val data = root["data"] as? JsonObject ?: return emptyList()
        val id = text(data["id"]) ?: "tiktok"

        val images = (data["images"] as? JsonArray)?.mapNotNull { text(it) }.orEmpty()
        if (images.isNotEmpty()) {
            return images.mapIndexed { index, url ->
                RemoteMedia(absolute(url), "$id-image-${(index + 1).toString().padStart(3, '0')}", ".jpg")
            }
        }

        val video = text(data["play"]) ?: text(data["wmplay"]) ?: return emptyList()
        return listOf(RemoteMedia(absolute(video), id, ".mp4"))
    }

    private fun text(element: Any?): String? =
        (element as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun absolute(url: String) = if (url.startsWith("/")) ORIGIN + url else url
}
```

`app/src/local/java/com/stillhere/app/local/PublicUrl.kt`:

```kotlin
package com.stillhere.app.local

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Guards URLs a third-party API handed back before we fetch them, so a hostile
 * upstream cannot point the phone at its own network — the home router, or a
 * Tailscale peer. Ported from the server's `ensure_public_http_url`.
 */
object PublicUrl {

    fun ensurePublic(
        url: String,
        resolve: (String) -> List<InetAddress> = { InetAddress.getAllByName(it).toList() },
    ) {
        val uri = runCatching { URI(url) }.getOrNull() ?: throw IllegalArgumentException("Media URL is not a URL")
        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            throw IllegalArgumentException("Media URL must start with http or https")
        }
        val host = uri.host?.trimEnd('.')?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Media URL has no host")
        val addresses = runCatching { resolve(host) }.getOrNull().orEmpty()
        if (addresses.isEmpty()) throw IllegalArgumentException("Media URL host could not be resolved")
        if (!addresses.all(::isPublic)) throw IllegalArgumentException("Media URL points at a non-public address")
    }

    fun isPublic(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isAnyLocalAddress || address.isMulticastAddress
        ) return false
        val bytes = address.address
        return when (address) {
            is Inet4Address -> {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                // 0/8 is "this network", 100.64/10 is carrier-grade NAT (where Tailscale
                // lives), and 240/4 is reserved.
                !(first == 0 || (first == 100 && second in 64..127) || first >= 240)
            }
            // fc00::/7 is IPv6's private range.
            is Inet6Address -> (bytes[0].toInt() and 0xfe) != 0xfc
            else -> false
        }
    }
}
```

`app/src/local/java/com/stillhere/app/local/MediaExtension.kt`:

```kotlin
package com.stillhere.app.local

import java.net.URI

/** Which extension a fetched file gets. Ported from the server's `_media_extension`. */
object MediaExtension {

    private val BY_TYPE = mapOf(
        "image/jpeg" to ".jpg",
        "image/png" to ".png",
        "image/webp" to ".webp",
        "video/mp4" to ".mp4",
        "audio/mpeg" to ".mp3",
    )
    private val KNOWN = setOf(".jpg", ".jpeg", ".png", ".webp", ".mp4", ".mp3")

    fun pick(contentType: String?, url: String, default: String): String {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase()
        BY_TYPE[type]?.let { return it }

        val path = runCatching { URI(url).path }.getOrNull().orEmpty()
        val suffix = path.substringAfterLast('/').let { name ->
            if ('.' in name) "." + name.substringAfterLast('.').lowercase() else ""
        }
        if (suffix in KNOWN) return if (suffix == ".jpeg") ".jpg" else suffix
        return default
    }
}
```

`app/src/local/java/com/stillhere/app/local/RemoteDownloader.kt`:

```kotlin
package com.stillhere.app.local

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Fetches the media URLs a fallback API handed back. Each is checked by [guard]
 * first and streamed to disk with a size ceiling rather than buffered whole —
 * the server's own guards, ported.
 */
class RemoteDownloader(
    private val client: OkHttpClient,
    private val guard: (String) -> Unit = { PublicUrl.ensurePublic(it) },
    private val maxBytes: Long = MAX_MEDIA_BYTES,
) {

    /** Download [media] into [dir] and return the file written. */
    fun download(media: RemoteMedia, dir: File, referer: String): File {
        guard(media.url)
        val request = Request.Builder()
            .url(media.url)
            .header("Referer", referer)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} fetching media")
            val body = response.body ?: throw IOException("Empty media response")
            val target = File(dir, media.stem + MediaExtension.pick(response.header("Content-Type"), media.url, media.defaultExtension))
            try {
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            if (written > maxBytes) throw IOException("Media is larger than ${maxBytes / MB} MB")
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } catch (e: IOException) {
                target.delete()
                throw e
            }
            return target
        }
    }

    companion object {
        const val MAX_MEDIA_BYTES = 1024L * 1024 * 1024
        private const val MB = 1024 * 1024

        // A mobile Chrome, as the login WebView presents itself.
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
```

- [ ] **Step 5: Run them to see them pass**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/local app/src/testLocal
git commit -m "feat: read tikwm answers and fetch fallback media behind the server's guards"
```

---

### Task 10: Bundle yt-dlp in the local flavor

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `app/src/local/java/com/stillhere/app/local/YtDlp.kt`

**Interfaces:**
- Consumes: `YtDlpOption` (Task 6), `EngineFailure` (Task 7), and the API names confirmed in the spike notes (Task 1).
- Produces:
  - `class YtDlp(context: Context)`
  - `suspend fun run(url: String, options: List<YtDlpOption>)`, which throws `EngineFailure`
  - `fun version(): String?`
  - `suspend fun updateIfIdle(): UpdateOutcome`
  - `sealed interface UpdateOutcome { Updated; AlreadyCurrent; Busy; Failed(message) }`

This task is Android-only glue with no JVM test. It is verified by compiling, then by the device smoke test in Task 15.

- [ ] **Step 1: Add the library to the local flavor only**

In `gradle/libs.versions.toml`, add the version and the two libraries **exactly as in Task 1 Step 3**, using the version recorded in the spike notes.

In `app/build.gradle.kts` `dependencies { }`:

```kotlin
    // Python, yt-dlp and ffmpeg as native libraries. Local only: the server
    // flavor must not carry tens of MB it never runs.
    "localImplementation"(libs.youtubedl.android.library)
    "localImplementation"(libs.youtubedl.android.ffmpeg)
```

- [ ] **Step 2: Packaging and ABI splits**

In `app/build.gradle.kts`, inside `android { }`:

```kotlin
    packaging {
        // youtubedl-android runs Python and ffmpeg as executables unpacked from
        // the APK's native libraries, which only works when they are extracted
        // at install. The server flavor has no native code, so this costs it nothing.
        jniLibs { useLegacyPackaging = true }
    }

    splits {
        abi {
            // Splits cannot be set per flavor, and the server flavor has no
            // native code to split. So split only when a Local build was asked for.
            isEnable = gradle.startParameter.taskNames.any { it.contains("Local") }
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
```

- [ ] **Step 3: Write the wrapper**

`app/src/local/java/com/stillhere/app/local/YtDlp.kt`. If the spike notes recorded a different stable-channel constant or `UpdateStatus` name, use those:

```kotlin
package com.stillhere.app.local

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface UpdateOutcome {
    data object Updated : UpdateOutcome
    data object AlreadyCurrent : UpdateOutcome
    /** A download holds yt-dlp; it cannot be replaced while it runs. */
    data object Busy : UpdateOutcome
    data class Failed(val message: String) : UpdateOutcome
}

/**
 * The one place that talks to youtubedl-android. One mutex covers downloads and
 * updates, because yt-dlp cannot be swapped out while it is running.
 */
class YtDlp(private val context: Context) {

    private val mutex = Mutex()

    @Volatile
    private var ready = false

    /** Unpacks Python on first use — seconds, once per install. */
    private fun ensureInit() {
        if (ready) return
        synchronized(this) {
            if (ready) return
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            ready = true
        }
    }

    suspend fun run(url: String, options: List<YtDlpOption>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureInit()
            val request = YoutubeDLRequest(url)
            options.forEach { option ->
                val value = option.value
                if (value == null) request.addOption(option.name) else request.addOption(option.name, value)
            }
            try {
                YoutubeDL.getInstance().execute(request)
            } catch (e: YoutubeDLException) {
                throw EngineFailure(e.message.orEmpty())
            }
        }
    }

    fun version(): String? = runCatching {
        ensureInit()
        YoutubeDL.getInstance().versionName(context)
    }.getOrNull()

    suspend fun updateIfIdle(): UpdateOutcome = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext UpdateOutcome.Busy
        try {
            ensureInit()
            when (YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)) {
                YoutubeDL.UpdateStatus.DONE -> UpdateOutcome.Updated
                else -> UpdateOutcome.AlreadyCurrent
            }
        } catch (e: Exception) {
            UpdateOutcome.Failed(e.message ?: "The update failed.")
        } finally {
            mutex.unlock()
        }
    }
}
```

- [ ] **Step 4: Build both flavors**

Build the two flavors in **separate** invocations. Splits switch on for the whole invocation whenever it names a Local task, so building both at once would split the server APK too:

```bash
./gradlew :app:assembleServerDebug
./gradlew :app:assembleLocalDebug
```

Expected: `BUILD SUCCESSFUL` both times.

Run: `ls app/build/outputs/apk/local/debug/ app/build/outputs/apk/server/debug/`
Expected: local has one APK per ABI (`app-local-arm64-v8a-debug.apk`, `app-local-armeabi-v7a-debug.apk`), and server has a single `app-server-debug.apk`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/local
git commit -m "feat: bundle yt-dlp and ffmpeg in the local flavor"
```

---

### Task 11: The local media source

**Files:**
- Create: `app/src/local/java/com/stillhere/app/local/LocalMediaSource.kt`
- Test: `app/src/testLocal/java/com/stillhere/app/local/LocalMediaSourceTest.kt`

**Interfaces:**
- Consumes: `MediaSource`, `IncomingFile`, `MediaException`, `MediaFiles` (Task 3), `Engine` and `LocalRoute` (Task 6), `EngineFailure` and `FriendlyError` (Task 7), `SessionCookies` (Task 5).
- Produces:
  - `data class Session(val sessionId: String, val cookieFile: File)`
  - `fun interface MediaEngine { suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) }`
  - `class LocalMediaSource(jobsRoot: File, engines: Map<Engine, MediaEngine>, sessionId: suspend (Platform) -> String?, now: () -> Long = System::currentTimeMillis) : MediaSource`

- [ ] **Step 1: Write the failing test**

`app/src/testLocal/java/com/stillhere/app/local/LocalMediaSourceTest.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalMediaSourceTest {

    @get:Rule val temp = TemporaryFolder()

    private val tiktok = UrlRouter.route("https://www.tiktok.com/@someone/video/1")!!
    private val instagram = UrlRouter.route("https://www.instagram.com/p/Cabc/")!!

    private val called = mutableListOf<Engine>()
    private val received = mutableListOf<Pair<IncomingFile, String>>()

    private fun writes(engine: Engine, vararg names: String) = MediaEngine { _, dir, _ ->
        called += engine
        names.forEach { File(dir, it).writeText(it) }
    }

    private fun fails(engine: Engine, raw: String) = MediaEngine { _, _, _ ->
        called += engine
        throw EngineFailure(raw)
    }

    private fun source(engines: Map<Engine, MediaEngine>, session: String? = null) =
        LocalMediaSource(temp.root, engines, sessionId = { session }, now = { 0L })

    private fun fetchAll(source: LocalMediaSource, routed: UrlRouter.Routed) = runBlocking {
        source.fetch(routed) { file, stream -> received += file to stream.readBytes().decodeToString() }
    }

    @Test
    fun theFirstEngineToProduceMediaWins() {
        fetchAll(source(mapOf(Engine.YT_DLP to writes(Engine.YT_DLP, "clip.mp4"), Engine.TIKWM to writes(Engine.TIKWM, "x.mp4"))), tiktok)
        assertEquals(listOf(Engine.YT_DLP), called)
        assertEquals(listOf(IncomingFile(1, 1, "clip.mp4", null) to "clip.mp4"), received)
    }

    @Test
    fun fallsBackAndHandsFilesOverInOrder() {
        run(
            source(mapOf(
                Engine.YT_DLP to fails(Engine.YT_DLP, "ERROR: Unsupported URL: x"),
                Engine.TIKWM to writes(Engine.TIKWM, "1-image-002.jpg", "1-image-001.jpg"),
            )),
            tiktok,
        )
        assertEquals(listOf(Engine.YT_DLP, Engine.TIKWM), called)
        assertEquals(listOf("1-image-001.jpg", "1-image-002.jpg"), received.map { it.first.name })
        assertEquals(listOf(1, 2), received.map { it.first.index })
        assertTrue(received.all { it.first.total == 2 })
    }

    @Test
    fun sidecarsNeverReachTheGallery() {
        fetchAll(source(mapOf(Engine.YT_DLP to writes(Engine.YT_DLP, "clip.mp4", "clip.info.json"), Engine.TIKWM to writes(Engine.TIKWM))), tiktok)
        assertEquals(listOf("clip.mp4"), received.map { it.first.name })
    }

    @Test
    fun onlySidecarsCountsAsNothingAndFallsThrough() {
        fetchAll(source(mapOf(Engine.YT_DLP to writes(Engine.YT_DLP, "clip.info.json"), Engine.TIKWM to writes(Engine.TIKWM, "clip.mp4"))), tiktok)
        assertEquals(listOf(Engine.YT_DLP, Engine.TIKWM), called)
    }

    @Test
    fun theFirstErrorIsTheOneShown() {
        val e = assertThrows(MediaException::class.java) {
            fetchAll(source(mapOf(
                Engine.YT_DLP to fails(Engine.YT_DLP, "ERROR: Unsupported URL: x"),
                Engine.TIKWM to fails(Engine.TIKWM, "tikwm said no"),
            )), tiktok)
        }
        assertEquals("That link isn't a post Still Here can save.", e.message)
    }

    @Test
    fun butNoVideoInAPhotoPostDefersToTheNextEngine() {
        val e = assertThrows(MediaException::class.java) {
            fetchAll(source(mapOf(
                Engine.YT_DLP to fails(Engine.YT_DLP, "ERROR: [Instagram] Cabc: There is no video in this post"),
                Engine.INSTAGRAM to fails(Engine.INSTAGRAM, "Sign in to Instagram to save photo posts."),
            )), instagram)
        }
        assertEquals("Sign in to Instagram to save photo posts.", e.message)
    }

    @Test
    fun enginesThatSucceedWithNothingSaySo() {
        val e = assertThrows(MediaException::class.java) {
            fetchAll(source(mapOf(Engine.YT_DLP to writes(Engine.YT_DLP), Engine.TIKWM to writes(Engine.TIKWM))), tiktok)
        }
        assertEquals("Nothing to save came back for that link.", e.message)
    }

    @Test
    fun theJobDirectoryIsGoneAfterwardsEitherWay() {
        fetchAll(source(mapOf(Engine.YT_DLP to writes(Engine.YT_DLP, "clip.mp4"), Engine.TIKWM to writes(Engine.TIKWM))), tiktok)
        runCatching { fetchAll(source(mapOf(Engine.YT_DLP to fails(Engine.YT_DLP, "x"), Engine.TIKWM to fails(Engine.TIKWM, "y"))), tiktok) }
        assertEquals(0, temp.root.listFiles()!!.size)
    }

    @Test
    fun aSessionArrivesAsACookieFileOnlyWhenThereIsOne() {
        var seen: Session? = null
        var cookieText: String? = null
        val capture = MediaEngine { _, dir, session ->
            seen = session
            cookieText = session?.cookieFile?.readText()
            File(dir, "a.jpg").writeText("a")
        }
        fetchAll(source(mapOf(Engine.YT_DLP to capture, Engine.INSTAGRAM to capture), session = "abc"), instagram)
        assertEquals("abc", seen!!.sessionId)
        assertTrue(cookieText!!.contains(".instagram.com\tTRUE\t/\tTRUE\t31536000\tsessionid\tabc"))

        fetchAll(source(mapOf(Engine.YT_DLP to capture, Engine.INSTAGRAM to capture), session = null), instagram)
        assertNull(seen)
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.LocalMediaSourceTest"`
Expected: compilation FAILS with `Unresolved reference: LocalMediaSource`.

- [ ] **Step 3: Implement**

`app/src/local/java/com/stillhere/app/local/LocalMediaSource.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.MediaFiles
import com.stillhere.app.domain.Platform
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import com.stillhere.app.download.MediaSource
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/** A platform login, as the engines need it: the raw value and a yt-dlp cookie file. */
data class Session(val sessionId: String, val cookieFile: File)

/** One way of turning a link into files in [outDir]. Throws [EngineFailure] when it can't. */
fun interface MediaEngine {
    suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?)
}

/**
 * Local's [MediaSource]: runs the engines [LocalRoute] picks, in order, in a scratch
 * directory that is deleted afterwards — cookie file included — whatever happens.
 */
class LocalMediaSource(
    private val jobsRoot: File,
    private val engines: Map<Engine, MediaEngine>,
    private val sessionId: suspend (Platform) -> String?,
    private val now: () -> Long = System::currentTimeMillis,
) : MediaSource {

    override suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit) {
        val jobDir = File(jobsRoot, UUID.randomUUID().toString())
        if (!jobDir.mkdirs()) throw MediaException("Couldn't make room to save this.")
        try {
            val files = runEngines(routed, jobDir, session(routed.platform, jobDir))
            files.forEachIndexed { index, file ->
                // No content type: MediaStoreSaver infers it from the extension.
                file.inputStream().use { onFile(IncomingFile(index + 1, files.size, file.name, null), it) }
            }
        } finally {
            jobDir.deleteRecursively()
        }
    }

    private suspend fun runEngines(routed: UrlRouter.Routed, jobDir: File, session: Session?): List<File> {
        var firstFailure: String? = null
        for (engine in LocalRoute.plan(routed)) {
            // Each engine gets its own directory so a failed attempt's leftovers
            // never reach the gallery.
            val outDir = File(jobDir, engine.name.lowercase()).apply { mkdirs() }
            try {
                engines.getValue(engine).fetchInto(routed, outDir, session)
                val media = outDir.walkTopDown()
                    .filter { it.isFile && MediaFiles.isMedia(it.name) }
                    .sortedBy { it.name }
                    .toList()
                if (media.isNotEmpty()) return media
            } catch (e: EngineFailure) {
                // The first error usually explains the post — except yt-dlp's
                // "no video" on a photo post, which only says it was the wrong tool.
                if (firstFailure == null && !FriendlyError.saysNoVideo(e.raw)) firstFailure = e.raw
            } catch (e: IOException) {
                if (firstFailure == null) firstFailure = e.message.orEmpty()
            }
        }
        throw MediaException(
            firstFailure?.let { FriendlyError.from(it, routed.platform) } ?: "Nothing to save came back for that link.",
        )
    }

    private suspend fun session(platform: Platform, jobDir: File): Session? {
        val id = sessionId(platform) ?: return null
        val text = SessionCookies.netscapeFile(platform, id, now() / 1000 + COOKIE_LIFETIME_SECONDS) ?: return null
        return Session(id, File(jobDir, "cookies.txt").apply { writeText(text) })
    }

    private companion object {
        const val COOKIE_LIFETIME_SECONDS = 365L * 24 * 60 * 60
    }
}
```

- [ ] **Step 4: Run it to see it pass**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.LocalMediaSourceTest"`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/local app/src/testLocal
git commit -m "feat: run local engines in order in a scratch directory that never outlives the job"
```

---

### Task 12: Real engines, wired into the local flavor

**Files:**
- Create: `app/src/local/java/com/stillhere/app/local/Engines.kt`
- Modify: `app/src/local/java/com/stillhere/app/FlavorModule.kt`

**Interfaces:**
- Consumes: everything from Tasks 5–11.
- Produces:
  - `YtDlpEngine(ytDlp: YtDlp)`, `TikwmEngine(client: OkHttpClient, downloader: RemoteDownloader)` and `InstagramEngine(client: OkHttpClient, downloader: RemoteDownloader)`, all implementing `MediaEngine`
  - local `FlavorModule.ytDlp: YtDlp`, `FlavorModule.scope: CoroutineScope`

- [ ] **Step 1: Write the engines**

`app/src/local/java/com/stillhere/app/local/Engines.kt`:

```kotlin
package com.stillhere.app.local

import com.stillhere.app.domain.UrlRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** yt-dlp, with the server's arguments. */
class YtDlpEngine(private val ytDlp: YtDlp) : MediaEngine {
    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) {
        ytDlp.run(routed.url, YtDlpArgs.build(routed.platform, outDir, session?.cookieFile))
    }
}

/** tikwm, for TikTok photo posts and whatever yt-dlp could not read. */
class TikwmEngine(
    private val client: OkHttpClient,
    private val downloader: RemoteDownloader,
) : MediaEngine {

    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) =
        withContext(Dispatchers.IO) {
            val url = API.toHttpUrl().newBuilder().addQueryParameter("url", routed.url).build()
            val body = get(Request.Builder().url(url).header("User-Agent", RemoteDownloader.USER_AGENT).build())
            val media = TikwmParser.parse(body)
            if (media.isEmpty()) throw EngineFailure("This post couldn't be read. It may be deleted or private.")
            media.forEach { fetch { downloader.download(it, outDir, "https://www.tiktok.com/") } }
        }

    private fun get(request: Request): String = fetch {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw EngineFailure("tikwm answered HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private companion object {
        const val API = "https://www.tikwm.com/api/"
    }
}

/** Instagram's media API, for photo posts and carousels. Needs a session. */
class InstagramEngine(
    private val client: OkHttpClient,
    private val downloader: RemoteDownloader,
) : MediaEngine {

    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) =
        withContext(Dispatchers.IO) {
            if (session == null) throw EngineFailure("Sign in to Instagram to save photo posts.")
            val shortcode = InstagramShortcode.fromUrl(routed.url)
                ?: throw EngineFailure("ERROR: Unsupported URL: ${routed.url}")
            val request = Request.Builder()
                .url("https://www.instagram.com/api/v1/media/${InstagramShortcode.toMediaId(shortcode)}/info/")
                .header("Cookie", "sessionid=${session.sessionId}")
                .header("X-IG-App-ID", WEB_APP_ID)
                .header("User-Agent", RemoteDownloader.USER_AGENT)
                .header("Referer", "https://www.instagram.com/")
                .build()
            val body = fetch {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 401 || response.code == 403 ->
                            throw EngineFailure("Your Instagram sign-in has expired. Sign in again.")
                        response.code == 404 -> throw EngineFailure("ERROR: HTTP Error 404: Not Found")
                        !response.isSuccessful -> throw EngineFailure("Instagram answered HTTP ${response.code}")
                        else -> response.body?.string().orEmpty()
                    }
                }
            }
            val media = InstagramMediaParser.parse(body, shortcode)
            if (media.isEmpty()) throw EngineFailure("Instagram returned nothing to save for this post.")
            media.forEach { fetch { downloader.download(it, outDir, "https://www.instagram.com/") } }
        }

    private companion object {
        // Instagram's own web client id; the media API refuses requests without one.
        const val WEB_APP_ID = "936619743392459"
    }
}

/** Network and guard failures become engine failures, so the source can fall back. */
private inline fun <T> fetch(block: () -> T): T = try {
    block()
} catch (e: IOException) {
    throw EngineFailure(e.message ?: "The connection failed.")
} catch (e: IllegalArgumentException) {
    throw EngineFailure(e.message ?: "The media URL was refused.")
}
```

- [ ] **Step 2: Wire the flavor**

Replace `app/src/local/java/com/stillhere/app/FlavorModule.kt` with:

```kotlin
package com.stillhere.app

import com.stillhere.app.auth.LocalSessionStore
import com.stillhere.app.auth.SessionStore
import com.stillhere.app.download.MediaSource
import com.stillhere.app.local.Engine
import com.stillhere.app.local.InstagramEngine
import com.stillhere.app.local.LocalMediaSource
import com.stillhere.app.local.RemoteDownloader
import com.stillhere.app.local.TikwmEngine
import com.stillhere.app.local.YtDlp
import com.stillhere.app.local.YtDlpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** The local flavor: everything happens on this phone. */
class FlavorModule(private val app: StillHereApp) : Flavor {
    val sessions: LocalSessionStore by lazy { LocalSessionStore(app) }
    val ytDlp: YtDlp by lazy { YtDlp(app) }

    /** For background work that outlives any screen, such as the daily yt-dlp check. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val jobsRoot get() = File(app.cacheDir, "jobs")

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override val features: Features = flavorFeatures
    override val sessionStore: SessionStore get() = sessions

    override val mediaSource: MediaSource by lazy {
        val downloader = RemoteDownloader(http)
        LocalMediaSource(
            jobsRoot = jobsRoot,
            engines = mapOf(
                Engine.YT_DLP to YtDlpEngine(ytDlp),
                Engine.TIKWM to TikwmEngine(http, downloader),
                Engine.INSTAGRAM to InstagramEngine(http, downloader),
            ),
            sessionId = sessions::sessionId,
        )
    }

    override fun onAppStart() {
        // A process that died mid-download leaves its job directory, cookie file
        // included. Nothing runs yet at process start, so all of it is stale.
        jobsRoot.deleteRecursively()
    }
}
```

- [ ] **Step 3: Build and test everything**

Run: `./gradlew :app:assembleServerDebug :app:assembleLocalDebug :app:testServerDebugUnitTest :app:testLocalDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests pass.

Run: `grep -rn "wired up in this build" app/src`
Expected: no output. The stand-in is gone.

- [ ] **Step 4: Commit**

```bash
git add app/src/local
git commit -m "feat: save posts on the phone with yt-dlp, tikwm and Instagram's media API"
```

---

### Task 13: Keep yt-dlp current

**Files:**
- Create: `app/src/local/java/com/stillhere/app/local/UpdatePolicy.kt`
- Create: `app/src/local/java/com/stillhere/app/local/YtDlpUpdater.kt`
- Create: `app/src/local/java/com/stillhere/app/ui/YtDlpSection.kt`
- Modify: `app/src/local/java/com/stillhere/app/FlavorModule.kt`, `app/src/local/java/com/stillhere/app/ui/FlavorScreens.kt`
- Test: `app/src/testLocal/java/com/stillhere/app/local/UpdatePolicyTest.kt`

**Interfaces:**
- Consumes: `YtDlp.updateIfIdle()`, `YtDlp.version()`, `UpdateOutcome` (Task 10), `FlavorModule.scope` (Task 12).
- Produces:
  - `UpdatePolicy.isDue(lastCheckMs: Long?, nowMs: Long): Boolean`
  - `class YtDlpUpdater(context: Context, ytDlp: YtDlp, scope: CoroutineScope)`, with `state: StateFlow<UpdaterState>`, `refreshVersion()`, `checkIfDue()` and `updateNow()`
  - `data class UpdaterState(val version: String? = null, val busy: Boolean = false, val message: String? = null)`
  - local `FlavorModule.updater: YtDlpUpdater`

- [ ] **Step 1: Write the failing test**

`app/src/testLocal/java/com/stillhere/app/local/UpdatePolicyTest.kt`:

```kotlin
package com.stillhere.app.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    private val day = UpdatePolicy.INTERVAL_MS

    @Test
    fun neverCheckedIsDue() = assertTrue(UpdatePolicy.isDue(null, 1_000))

    @Test
    fun withinADayIsNot() = assertFalse(UpdatePolicy.isDue(10_000, 10_000 + day - 1))

    @Test
    fun aDayLaterIs() = assertTrue(UpdatePolicy.isDue(10_000, 10_000 + day))

    @Test
    fun aClockThatWentBackwardsIsDue() = assertTrue(UpdatePolicy.isDue(10_000, 5_000))
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.UpdatePolicyTest"`
Expected: compilation FAILS with `Unresolved reference: UpdatePolicy`.

- [ ] **Step 3: Implement the policy**

`app/src/local/java/com/stillhere/app/local/UpdatePolicy.kt`:

```kotlin
package com.stillhere.app.local

/** How often Local checks for a newer yt-dlp by itself. */
object UpdatePolicy {
    const val INTERVAL_MS = 24L * 60 * 60 * 1000

    fun isDue(lastCheckMs: Long?, nowMs: Long): Boolean =
        lastCheckMs == null || nowMs < lastCheckMs || nowMs - lastCheckMs >= INTERVAL_MS
}
```

- [ ] **Step 4: Run it to see it pass**

Run: `./gradlew :app:testLocalDebugUnitTest --tests "com.stillhere.app.local.UpdatePolicyTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Write the updater**

`app/src/local/java/com/stillhere/app/local/YtDlpUpdater.kt`:

```kotlin
package com.stillhere.app.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.ytDlpDataStore: DataStore<Preferences> by preferencesDataStore(name = "ytdlp")

data class UpdaterState(
    val version: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Keeps the bundled yt-dlp current: once a day by itself, or when asked. */
class YtDlpUpdater(
    private val context: Context,
    private val ytDlp: YtDlp,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(UpdaterState())
    val state: StateFlow<UpdaterState> = _state.asStateFlow()

    fun refreshVersion() {
        scope.launch { _state.update { it.copy(version = ytDlp.version()) } }
    }

    /** At most once a day, in the background. A running download defers it to next launch. */
    fun checkIfDue() {
        scope.launch {
            val last = context.ytDlpDataStore.data.first()[KEY_LAST_CHECK]
            if (!UpdatePolicy.isDue(last, System.currentTimeMillis())) return@launch
            if (update() !is UpdateOutcome.Busy) markChecked()
        }
    }

    fun updateNow() {
        scope.launch { if (update() !is UpdateOutcome.Busy) markChecked() }
    }

    private suspend fun update(): UpdateOutcome {
        _state.update { it.copy(busy = true, message = null) }
        val outcome = ytDlp.updateIfIdle()
        _state.value = UpdaterState(version = ytDlp.version(), busy = false, message = describe(outcome))
        return outcome
    }

    private suspend fun markChecked() {
        context.ytDlpDataStore.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
    }

    private fun describe(outcome: UpdateOutcome): String = when (outcome) {
        UpdateOutcome.Updated -> "Updated."
        UpdateOutcome.AlreadyCurrent -> "Already the newest."
        UpdateOutcome.Busy -> "A save is running. Try again when it's done."
        is UpdateOutcome.Failed -> "Couldn't update: ${outcome.message}"
    }

    private companion object {
        val KEY_LAST_CHECK = longPreferencesKey("last_update_check")
    }
}
```

- [ ] **Step 6: Add it to the flavor**

In the local `FlavorModule.kt`, add `import com.stillhere.app.local.YtDlpUpdater` and this property below `ytDlp`:

```kotlin
    val updater: YtDlpUpdater by lazy { YtDlpUpdater(app, ytDlp, scope) }
```

Add this line to the end of `onAppStart()`:

```kotlin
        updater.checkIfDue()
```

- [ ] **Step 7: Show it in the sheet**

`app/src/local/java/com/stillhere/app/ui/YtDlpSection.kt`:

```kotlin
package com.stillhere.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.StillHereApp
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerType

/** The Engine section of the sheet: which yt-dlp this phone runs, and a way to update it. */
@Composable
fun YtDlpSection() {
    val updater = (LocalContext.current.applicationContext as StillHereApp).flavor.updater
    val state by updater.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { updater.refreshVersion() }

    FiledHead("Engine")
    Spacer(Modifier.height(10.dp))
    Text(
        "Posts are saved on this phone by yt-dlp. When a site changes and saving " +
            "stops working, an update usually fixes it.",
        style = MaterialTheme.typography.bodyMedium,
        color = Ledger.Dim,
    )
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "yt-dlp ${state.version ?: "…"}",
            style = MaterialTheme.typography.titleMedium,
            color = Ledger.Ink,
            modifier = Modifier.weight(1f),
        )
        LedgerButton(if (state.busy) "Updating…" else "Update now", { updater.updateNow() }, enabled = !state.busy)
    }
    state.message?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = LedgerType.label, color = Ledger.Dim)
    }
}
```

In `app/src/local/java/com/stillhere/app/ui/FlavorScreens.kt`, replace `FlavorSettingsSection` with:

```kotlin
/** Local's settings, at the top of the Sessions sheet. */
@Composable
fun FlavorSettingsSection(onDone: () -> Unit) = YtDlpSection()
```

- [ ] **Step 8: Build and test**

Run: `./gradlew :app:assembleLocalDebug :app:testLocalDebugUnitTest :app:assembleServerDebug`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 9: Commit**

```bash
git add app/src/local app/src/testLocal
git commit -m "feat: keep yt-dlp current, daily and on demand"
```

---

### Task 14: Docs

**Files:**
- Modify: `README.md`, `docs/open-items.md`, `docs/superpowers/specs/2026-09-24-local-flavor-design.md`

- [ ] **Step 1: README**

In `README.md`, directly after the intro paragraph that ends "Android only.", add:

```markdown
## Two apps, one codebase

| | **Still Here** (`server`) | **Still Here Local** (`local`) |
|---|---|---|
| Does the work | your `ttl-downloader` backend | this phone, with a bundled yt-dlp |
| Save posts (TikTok, Instagram, YouTube) | yes | yes |
| Record a live | yes | not yet |
| Auto-record, shared register | yes | no |
| Sessions stored | on the backend | on the phone only |
| Setup | a backend URL | none |

Both install side by side (`com.stillhere.app` and `com.stillhere.app.local`).
Local falls back to tikwm.com for TikTok photo posts and when yt-dlp can't read
a TikTok link, and to Instagram's media API for photo posts. The media API needs
an Instagram sign-in.
```

In **Setup → 2. Build the APK**, replace the `assembleDebug` line and the output path with:

```powershell
.\gradlew.bat assembleServerDebug   # app/build/outputs/apk/server/debug/app-server-debug.apk
.\gradlew.bat assembleLocalDebug    # app/build/outputs/apk/local/debug/app-local-<abi>-debug.apk
```

Add a note under it: `Build each flavor in its own command. Local ships one APK per ABI; most phones want arm64-v8a.`

Change the first sentence of **3. Install and configure** to start with `For the server app:`, and add after that section: `Still Here Local needs no configuration.`

Replace the **Project structure** tree's package path `com/ttldownloader/app/` with `com/stillhere/app/`. Add a line under it: `src/main is shared; src/server talks to the backend; src/local runs yt-dlp. Nothing in main or local may import the net package.`

- [ ] **Step 2: open-items**

In `docs/open-items.md`, under **State of play**, add:

```markdown
**Two flavors since 2026-09-24.** `server` is this app and is unchanged in
behaviour, so the server-side shim removal it gates still stands. `local` is
new — see `docs/superpowers/specs/2026-09-24-local-flavor-design.md`. Its
device smoke test is item 2.4 below.
```

Under section 2, add:

```markdown
### 2.4 Smoke-test Still Here Local

Install `app-local-arm64-v8a-debug.apk` beside the server app and check:
TikTok video · TikTok photo post · Instagram reel · Instagram carousel (signed
in) · Instagram photo post signed out (expect "Sign in to Instagram…") ·
YouTube Short · a long YouTube video (tests the audio/video merge) ·
Sessions → Update now.
```

- [ ] **Step 3: Bring the spec in line with what was built**

In `docs/superpowers/specs/2026-09-24-local-flavor-design.md`:
- In **Seams**, replace the two interface code blocks with the signatures from Task 3's Interfaces block: `MediaSource.fetch(routed, onFile: (IncomingFile, InputStream) -> Unit)`, and `SessionStore` with `isAvailable()`.
- Replace the `Features` code block with `data class Features(val destinations: List<Destination>, val sessionsNote: String)`. Add one sentence: *Server settings and Local's yt-dlp row are flavor composables (`FlavorSettingsSection`), not flags.*
- In **Errors**, add after the table: *Exception: yt-dlp's "There is no video in this post" never counts as the first error — on a photo post it only means yt-dlp was the wrong tool, and the next engine's error explains the post.*
- In **3. Extract → tikwm**, replace "`data.play`" with "`data.play`, falling back to the watermarked `data.wmplay` (the server prefers `wmplay`; Local prefers the clean copy)". Add: *Relative paths are resolved against `https://www.tikwm.com`.*

- [ ] **Step 4: Commit**

```bash
git add README.md docs/open-items.md docs/superpowers/specs/2026-09-24-local-flavor-design.md
git commit -m "docs: describe the two flavors and bring the spec in line with the build"
```

---

### Task 15: Device smoke test

**Needs a device**, as in Task 1.

- [ ] **Step 1: Install both apps**

```bash
./gradlew :app:assembleServerDebug
./gradlew :app:assembleLocalDebug
adb install -r app/build/outputs/apk/server/debug/app-server-debug.apk
adb install -r app/build/outputs/apk/local/debug/app-local-arm64-v8a-debug.apk
```

Expected: two icons, **Still Here** and **Still Here Local**, the second on pale green paper.

- [ ] **Step 2: Check the server app is unchanged**

Open Still Here and confirm:
- the three tabs are there
- Sessions shows the Server section
- saving one TikTok link works through the backend

- [ ] **Step 3: Work through the local checklist**

Open Still Here Local and confirm:
- there's no nav strip
- Sessions shows the Engine section with a yt-dlp version

Then share each of these into **Still Here Local** from its own app, and check the result in the gallery under `Movies/StillHere` or `Pictures/StillHere`:

1. TikTok video
2. TikTok photo post (several images, in order)
3. Instagram reel, signed out
4. Instagram photo post, signed out. Expect *"Sign in to Instagram to save photo posts."*
5. Sign in to Instagram in Sessions, then save an Instagram carousel (images and videos, in order)
6. YouTube Short
7. A YouTube video over 5 minutes. Expect a single playable MP4 with sound.
8. Sessions → **Update now**. Expect a message and a version number.
9. Start a save, leave the app, and confirm the notification finishes.

Afterwards, run `adb shell run-as com.stillhere.app.local ls cache/jobs`. Expected: an empty result or "No such file". Leftover job directories mean the cleanup failed.

- [ ] **Step 4: Record the results**

In `docs/open-items.md` item 2.4, mark each check pass or fail. For each failure, add the message the app showed. Commit:

```bash
git add docs/open-items.md
git commit -m "docs: record the Still Here Local device run"
```

Any failure becomes a new task: use the systematic-debugging skill before fixing it.
