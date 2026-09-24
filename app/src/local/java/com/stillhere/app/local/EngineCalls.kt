package com.stillhere.app.local

import java.io.IOException

/** Network and guard failures become engine failures, so the source can fall back. */
internal inline fun <T> engineCall(block: () -> T): T = try {
    block()
} catch (e: IOException) {
    throw EngineFailure(e.message ?: "The connection failed.")
} catch (e: IllegalArgumentException) {
    throw EngineFailure(e.message ?: "The media URL was refused.")
}
