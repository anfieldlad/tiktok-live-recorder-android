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
