package com.stillhere.app.local

/**
 * One file a fallback engine should fetch. The file is written as [stem] plus an
 * extension read from the response, or [defaultExtension] when it says nothing.
 */
data class RemoteMedia(val url: String, val stem: String, val defaultExtension: String)
