package com.stillhere.app.domain

/** Real media we want in the gallery — not the sidecars and leftovers extractors write. */
object MediaFiles {
    private val NOT_MEDIA = listOf(".json", ".txt", ".nfo", ".part", ".ytdl")

    fun isMedia(name: String?): Boolean {
        val file = name?.substringAfterLast('/')?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
        return NOT_MEDIA.none { file.endsWith(it) }
    }
}
