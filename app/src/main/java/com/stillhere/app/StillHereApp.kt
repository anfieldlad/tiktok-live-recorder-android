package com.stillhere.app

import android.app.Application
import com.stillhere.app.data.HistoryRepo
import com.stillhere.app.data.MediaStoreSaver
import com.stillhere.app.data.SettingsRepo
import com.stillhere.app.download.DownloadController
import com.stillhere.app.download.DownloadManager
import com.stillhere.app.live.LiveController
import com.stillhere.app.net.ApiClient

/**
 * Tiny manual DI container. The app is small enough that lazy singletons on the
 * Application beat pulling in a DI framework. Reach them via
 * `(context.applicationContext as StillHereApp)`.
 */
class StillHereApp : Application() {
    val settings: SettingsRepo by lazy { SettingsRepo(this) }
    val history: HistoryRepo by lazy { HistoryRepo(this) }
    val api: ApiClient by lazy { ApiClient(settings) }
    val saver: MediaStoreSaver by lazy { MediaStoreSaver(this) }
    val downloadManager: DownloadManager by lazy { DownloadManager(api, saver, history) }

    /** Process-wide holder for the currently active download's progress. */
    val controller: DownloadController by lazy { DownloadController() }

    /** Process-wide holder for the active live recording. */
    val liveController: LiveController by lazy { LiveController() }
}
