package com.ttldownloader.app

import android.app.Application
import com.ttldownloader.app.data.HistoryRepo
import com.ttldownloader.app.data.MediaStoreSaver
import com.ttldownloader.app.data.SettingsRepo
import com.ttldownloader.app.download.DownloadController
import com.ttldownloader.app.download.DownloadManager
import com.ttldownloader.app.net.ApiClient

/**
 * Tiny manual DI container. The app is small enough that lazy singletons on the
 * Application beat pulling in a DI framework. Reach them via
 * `(context.applicationContext as TtlApp)`.
 */
class TtlApp : Application() {
    val settings: SettingsRepo by lazy { SettingsRepo(this) }
    val history: HistoryRepo by lazy { HistoryRepo(this) }
    val api: ApiClient by lazy { ApiClient(settings) }
    val saver: MediaStoreSaver by lazy { MediaStoreSaver(this) }
    val downloadManager: DownloadManager by lazy { DownloadManager(api, saver, history) }

    /** Process-wide holder for the currently active download's progress. */
    val controller: DownloadController by lazy { DownloadController() }
}
