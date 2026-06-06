package com.ttldownloader.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.InputStream

/** A media file that was written into the device gallery. */
data class SavedItem(
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean,
)

/**
 * Writes downloaded bytes straight into the device gallery via MediaStore.
 *
 * On Android 10+ (the app's minSdk is 26, but scoped-storage inserts work from 29)
 * this needs no runtime storage permission. Videos land in `Movies/TTLDownloader`,
 * images in `Pictures/TTLDownloader`, so they show up in the gallery automatically.
 */
class MediaStoreSaver(private val context: Context) {

    fun save(filename: String, contentType: String?, input: InputStream): SavedItem {
        val isVideo = looksLikeVideo(filename, contentType)
        val mime = mimeFor(filename, contentType, isVideo)
        val resolver = context.contentResolver

        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val relativeDir = if (isVideo) {
            "${Environment.DIRECTORY_MOVIES}/$SUBFOLDER"
        } else {
            "${Environment.DIRECTORY_PICTURES}/$SUBFOLDER"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(collection, values)
            ?: error("MediaStore refused to create an entry for $filename")

        try {
            resolver.openOutputStream(itemUri)?.use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            } ?: error("Could not open output stream for $filename")
        } catch (t: Throwable) {
            // Roll back the half-written pending entry so it does not linger.
            runCatching { resolver.delete(itemUri, null, null) }
            throw t
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }

        return SavedItem(itemUri, filename, isVideo)
    }

    private fun looksLikeVideo(filename: String, contentType: String?): Boolean {
        if (contentType != null && contentType.startsWith("video/", ignoreCase = true)) return true
        if (contentType != null && contentType.startsWith("image/", ignoreCase = true)) return false
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "mp4", "mov", "webm", "mkv", "m4v", "3gp" -> true
            else -> false
        }
    }

    private fun mimeFor(filename: String, contentType: String?, isVideo: Boolean): String {
        // The backend serves application/octet-stream, so prefer the extension.
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "3gp" -> "video/3gpp"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            else -> contentType?.takeIf { it != "application/octet-stream" }
                ?: if (isVideo) "video/mp4" else "image/jpeg"
        }
    }

    private companion object {
        const val SUBFOLDER = "TTLDownloader"
    }
}
