package com.stillhere.app.ui

import androidx.compose.runtime.Composable

/** Local has no tabs of its own in phase 1; Save post is shared. */
@Composable
fun FlavorDestination(destination: Destination) = Unit

/** Local's settings, at the top of the Sessions sheet. Filled in by the yt-dlp task. */
@Composable
fun FlavorSettingsSection(onDone: () -> Unit) = Unit
