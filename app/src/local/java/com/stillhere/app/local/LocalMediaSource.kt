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
