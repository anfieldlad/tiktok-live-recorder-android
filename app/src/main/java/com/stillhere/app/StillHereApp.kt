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
