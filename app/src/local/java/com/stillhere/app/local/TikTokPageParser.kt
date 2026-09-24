package com.stillhere.app.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.net.URI

/**
 * Reads a TikTok post page. TikTok serves the post in one of two shapes,
 * depending on the cookies the request already carries, so both are tried.
 */
object TikTokPageParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** The post's `itemStruct`, or null when the page holds no post. */
    fun itemStruct(html: String): JsonObject? = fromRehydration(html) ?: fromApiData(html)

    /** What to save: a photo post's images in order, or else the best video. */
    fun media(item: JsonObject): List<RemoteMedia> {
        val id = text(item["id"]) ?: "tiktok"

        // A photo post also carries a video (its soundtrack); the images are the post.
        val images = ((item["imagePost"] as? JsonObject)?.get("images") as? JsonArray)
            ?.mapNotNull { image ->
                val urls = ((image as? JsonObject)?.get("imageURL") as? JsonObject)?.get("urlList") as? JsonArray
                urls?.firstOrNull()?.let(::text)
            }
            .orEmpty()
        if (images.isNotEmpty()) {
            return images.mapIndexed { index, url ->
                RemoteMedia(url, "$id-image-${(index + 1).toString().padStart(3, '0')}", ".jpg")
            }
        }

        val video = item["video"] as? JsonObject ?: return emptyList()
        val url = bestBitrate(video) ?: text(video["playAddr"]) ?: text(video["downloadAddr"]) ?: return emptyList()
        return listOf(RemoteMedia(url, id, ".mp4"))
    }

    /**
     * Whether a resolved link is a post. An expired short link does not 404: it
     * redirects to the homepage with HTTP 200, so only the path tells.
     */
    fun isPostUrl(url: String): Boolean {
        val segments = runCatching { URI(url).path }.getOrNull().orEmpty().split('/')
        val kind = segments.indexOfFirst { it == "video" || it == "photo" }
        return kind > 0 && segments.getOrNull(kind + 1)?.isNotBlank() == true
    }

    // Cookie-primed requests: the item sits in the rehydration script, under the
    // one `…-detail` key that isn't a reflow key.
    private fun fromRehydration(html: String): JsonObject? {
        val scope = parse(script(html, "__UNIVERSAL_DATA_FOR_REHYDRATION__"))?.get("__DEFAULT_SCOPE__") as? JsonObject
        val detail = scope?.entries?.firstOrNull { (key, _) -> key.endsWith("-detail") && "reflow" !in key }?.value
        return itemOf(detail)
    }

    // A first, cookie-less request: the item sits in its own `api-data` script,
    // under `videoDetail` — for photo posts too.
    private fun fromApiData(html: String): JsonObject? {
        val root = parse(script(html, "api-data"))
        val detail = root?.entries?.firstOrNull { (key, _) -> key.endsWith("Detail") }?.value
        return itemOf(detail)
    }

    private fun itemOf(detail: JsonElement?): JsonObject? =
        ((detail as? JsonObject)?.get("itemInfo") as? JsonObject)?.get("itemStruct") as? JsonObject

    private fun bestBitrate(video: JsonObject): String? =
        (video["bitrateInfo"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.maxByOrNull { (it["Bitrate"] as? JsonPrimitive)?.longOrNull ?: 0 }
            ?.let { ((it["PlayAddr"] as? JsonObject)?.get("UrlList") as? JsonArray)?.firstOrNull() }
            ?.let(::text)

    private fun script(html: String, id: String): String? =
        Regex("""<script[^>]*\bid="${Regex.escape(id)}"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)

    private fun parse(text: String?): JsonObject? =
        text?.let { runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull() }

    private fun text(element: JsonElement?): String? =
        (element as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
