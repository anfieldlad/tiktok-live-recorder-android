package com.stillhere.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFilesTest {

    @Test
    fun keepsMedia() {
        assertTrue(MediaFiles.isMedia("clip-7300000000000000000.mp4"))
        assertTrue(MediaFiles.isMedia("/downloads/abc/files/image-001.JPG"))
    }

    @Test
    fun dropsSidecarsAndLeftovers() {
        listOf("a.info.json", "meta.JSON", "notes.txt", "x.nfo", "v.mp4.part", "v.mp4.ytdl").forEach {
            assertFalse(it, MediaFiles.isMedia(it))
        }
    }

    @Test
    fun dropsNothing() {
        assertFalse(MediaFiles.isMedia(null))
        assertFalse(MediaFiles.isMedia(""))
        assertFalse(MediaFiles.isMedia("some/dir/"))
    }
}
