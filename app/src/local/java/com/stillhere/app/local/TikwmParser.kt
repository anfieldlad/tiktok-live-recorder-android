package com.stillhere.app.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Reads tikwm's `/api/?url=` answer: a photo post's images, or else the video.
 * Ported from the server's `_download_with_metadata_fallback`, except that the
 * clean `play` copy is preferred to the watermarked `wmplay` one.
 */
object TikwmParser {

    private const val ORIGIN = "https://www.tikwm.com"
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): List<RemoteMedia> {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return emptyList()
        val data = root["data"] as? JsonObject ?: return emptyList()
        val id = text(data["id"]) ?: "tiktok"

        val images = (data["images"] as? JsonArray)?.mapNotNull { text(it) }.orEmpty()
        if (images.isNotEmpty()) {
            return images.mapIndexed { index, url ->
                RemoteMedia(absolute(url), "$id-image-${(index + 1).toString().padStart(3, '0')}", ".jpg")
            }
        }

        val video = text(data["play"]) ?: text(data["wmplay"]) ?: return emptyList()
        return listOf(RemoteMedia(absolute(video), id, ".mp4"))
    }

    private fun text(element: JsonElement?): String? =
        (element as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun absolute(url: String) = if (url.startsWith("/")) ORIGIN + url else url
}
