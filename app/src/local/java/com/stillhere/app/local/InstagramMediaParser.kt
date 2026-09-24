package com.stillhere.app.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Reads `api/v1/media/{id}/info/` into the files a post is made of, in order. */
object InstagramMediaParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String, shortcode: String): List<RemoteMedia> {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return emptyList()
        val item = (root["items"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return emptyList()
        val parts = (item["carousel_media"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: listOf(item)
        return parts.mapIndexedNotNull { index, part ->
            val stem = "$shortcode-${(index + 1).toString().padStart(2, '0')}"
            // A video item also carries images (its thumbnail); the video is the post.
            largest(part["video_versions"] as? JsonArray)?.let { RemoteMedia(it, stem, ".mp4") }
                ?: largest((part["image_versions2"] as? JsonObject)?.get("candidates") as? JsonArray)
                    ?.let { RemoteMedia(it, stem, ".jpg") }
        }
    }

    private fun largest(versions: JsonArray?): String? =
        versions?.mapNotNull { it as? JsonObject }
            ?.maxByOrNull { area(it) }
            ?.get("url")?.jsonPrimitive?.contentOrNull

    private fun area(version: JsonObject): Long {
        val width = version["width"]?.jsonPrimitive?.longOrNull ?: 0
        val height = version["height"]?.jsonPrimitive?.longOrNull ?: 0
        return width * height
    }
}
