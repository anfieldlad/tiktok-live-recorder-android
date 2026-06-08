# TTL Downloader — Android

A small, native Android client for the self-hosted [`ttl-downloader`](../ttl-downloader) backend. It
sends TikTok and Instagram links to your own server (which runs `yt-dlp` / `gallery-dl`) and saves the
returned media straight into your phone's gallery.

This is a **thin client** — it does no extraction itself. The downloading is done by the backend; the app
just hands it a URL and streams the result to the gallery. It's built for **personal / sideload** use
(no Play Store), Android only.

## Features

- **Share-sheet import** — in TikTok or Instagram, tap **Share → TTL Downloader** and the download starts
  automatically. No copy-paste.
- **Save to gallery** — videos land in `Movies/TTLDownloader`, images in `Pictures/TTLDownloader`, and
  show up in your gallery app. No storage permission needed (scoped storage / MediaStore).
- **Clipboard auto-detect** — copy a TikTok/Instagram link, open the app, and a one-tap banner offers to
  download it.
- **Foreground service** — downloads keep running (with a progress notification) even if you leave the app,
  which matters because server-side downloads can take 30–120s.

## How it talks to the backend

| Action | Request |
| --- | --- |
| Start a TikTok download | `POST {baseUrl}/downloads` with `{ "url": "..." }` |
| Start an Instagram download | `POST {baseUrl}/instagram/downloads` with `{ "url": "..." }` |
| Fetch a resulting file | `GET {baseUrl}{file_url}` (paths come back in `file_urls`) |

Platform is chosen automatically from the link's hostname. Each file is fetched exactly once — the
Instagram endpoint deletes its server-side copy after serving. An optional `X-API-Key` header is sent when
an API key is configured in Settings.

## Setup

### 1. Run the backend reachable from your phone

In the `ttl-downloader` repo, bind to all interfaces and keep it on a private network the phone can reach
(Tailscale recommended):

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 2. Build the APK

Requirements: **JDK 17+** and the **Android SDK** (platform + build-tools 36). Open the project in
**Android Studio** (it brings its own JDK and handles the SDK), or build from the command line:

```powershell
# Point Gradle at a JDK 17+ (Java 8 will NOT work with this AGP version)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.4.1"
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

> `local.properties` (git-ignored) must point `sdk.dir` at your Android SDK. Android Studio writes this
> automatically; the committed example points at the default Windows location.

### 3. Install and configure

```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
```

On first launch, open **Settings** and set the **Backend URL** (e.g. `http://100.x.y.z:8000`). Leave the
API key blank unless your server requires one.

## Project structure

```
app/src/main/java/com/ttldownloader/app/
  TtlApp.kt                 Application + tiny manual-DI container (lazy singletons)
  MainActivity.kt           Compose host; clipboard check on resume; ACTION_VIEW links
  ShareReceiverActivity.kt  ACTION_SEND target (the share-sheet entry point)
  net/
    DownloadModels.kt       Platform enum + response/error models
    ApiClient.kt            OkHttp client: createDownload(), withFile()
  domain/UrlRouter.kt       Extract a URL from shared text + route by hostname
  download/
    DownloadManager.kt      Orchestrates POST -> stream files -> gallery; emits progress
    DownloadController.kt    Process-wide StateFlow of the active download
    DownloadService.kt      Foreground service that runs a download + notification
  data/
    SettingsRepo.kt         DataStore: backend URL + API key
    HistoryRepo.kt          DataStore: recent downloads (JSON)
    MediaStoreSaver.kt      Writes bytes into the gallery via MediaStore
  ui/
    AppViewModel.kt         State for the screens
    AppUi.kt                Home + Settings + progress/clipboard/history composables
    theme/Theme.kt          Material 3 theme
```

## Tech

Native Kotlin · Jetpack Compose (Material 3) · OkHttp · kotlinx.serialization · DataStore · Coroutines.
`minSdk 26`, `compileSdk/targetSdk 36`, AGP 8.9, Gradle 8.13.

## Notes

- **Cleartext HTTP is allowed.** The backend is served over plain `http://` on a private network, so the
  app sets `android:usesCleartextTraffic="true"`. Android blocks cleartext by default; this is required
  to reach an HTTP backend (and is acceptable for a personal app on a trusted network/Tailscale).
- **No login flow.** The app inherits whatever session cookies the *server* already has. Private or
  age-gated content depends on the backend's configuration, not the phone.
- **Personal use.** Downloader apps are routinely rejected from the Play Store; this is meant to be
  sideloaded.
