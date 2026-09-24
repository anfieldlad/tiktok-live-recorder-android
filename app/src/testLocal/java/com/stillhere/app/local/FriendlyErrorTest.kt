package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendlyErrorTest {

    private fun tiktok(raw: String) = FriendlyError.from(raw, Platform.TIKTOK)
    private fun instagram(raw: String) = FriendlyError.from(raw, Platform.INSTAGRAM)

    @Test
    fun deadTikTokPostIsGoneNotABugReport() = assertEquals(
        "This post is gone — deleted, private, or region-locked.",
        tiktok(
            "ERROR: [TikTok] 7300: Unexpected response from webpage request; please report this issue on " +
                " https://github.com/yt-dlp/yt-dlp/issues?q= , filling out the appropriate issue template.",
        ),
    )

    @Test
    fun instagramLoginWallAsksForASignIn() = assertEquals(
        "Sign in to Instagram to save this.",
        instagram(
            "ERROR: [Instagram] Cabc: Requested content is not available, rate-limit reached or login required. " +
                "Use --cookies, --cookies-from-browser, --username and --password",
        ),
    )

    @Test
    fun youtubeLoginWallSaysThereIsNoLoginToOffer() = assertEquals(
        "YouTube won't serve this without a login, which Still Here doesn't keep.",
        FriendlyError.from("ERROR: [youtube] abc: Sign in to confirm your age.", Platform.YOUTUBE),
    )

    @Test
    fun noNetwork() = assertEquals(
        "No connection. Check your network and try again.",
        tiktok("ERROR: Unable to download webpage: <urlopen error [Errno 7] No address associated with hostname>"),
    )

    @Test
    fun rateLimited() = assertEquals(
        "TikTok is rate-limiting. Try again in a few minutes.",
        tiktok("ERROR: Unable to download webpage: HTTP Error 429: Too Many Requests"),
    )

    @Test
    fun unsupportedLink() = assertEquals(
        "That link isn't a post Still Here can save.",
        tiktok("ERROR: Unsupported URL: https://www.tiktok.com/@someone"),
    )

    @Test
    fun brokenExtractorSuggestsAnUpdate() = assertEquals(
        "TikTok did not return this post. ${FriendlyError.UPDATE_HINT}",
        tiktok("ERROR: [TikTok] 1: Unable to extract universal data for rehydration; please report this issue on https://github.com/yt-dlp/yt-dlp/issues"),
    )

    @Test
    fun anythingElseIsItsLastLineWithoutThePrefix() =
        assertEquals("something odd happened", tiktok("WARNING: foo\nERROR: something odd happened\n"))

    @Test
    fun silence() = assertEquals("The download failed without saying why.", tiktok("  "))

    @Test
    fun recognisesAPostWithNoVideo() {
        assertTrue(FriendlyError.saysNoVideo("ERROR: [Instagram] Cabc: There is no video in this post"))
        assertFalse(FriendlyError.saysNoVideo("ERROR: Unsupported URL: x"))
    }

    @Test
    fun instagramFollowersOnlyLoginWallAsksForASignIn() = assertEquals(
        "Sign in to Instagram to save this.",
        instagram(
            "ERROR: [Instagram] Cabc: This content is only available for registered users who follow this " +
                "account. Use --cookies, --cookies-from-browser, --username and --password, --netrc-cmd, or " +
                "--netrc (instagram) to provide account credentials. See  " +
                "https://github.com/yt-dlp/yt-dlp/wiki/FAQ#how-do-i-pass-cookies-to-yt-dlp  for how to " +
                "manually pass cookies",
        ),
    )

    @Test
    fun instagramRedirectedToLoginPageAsksForASignIn() = assertEquals(
        "Sign in to Instagram to save this.",
        instagram(
            "ERROR: [Instagram] Cabc: The webpage request was redirected to the login page. You have " +
                "exceeded the rate-limit for accessing posts anonymously. Use --cookies, " +
                "--cookies-from-browser, --username and --password, --netrc-cmd, or --netrc (instagram) to " +
                "provide account credentials. See  " +
                "https://github.com/yt-dlp/yt-dlp/wiki/FAQ#how-do-i-pass-cookies-to-yt-dlp  for how to " +
                "manually pass cookies",
        ),
    )

    @Test
    fun tikTokPrivateAccountLoginWallAsksForASignIn() = assertEquals(
        "Sign in to TikTok to save this.",
        tiktok(
            "ERROR: [TikTok] Cabc: This user's account is likely either private or all of their videos are " +
                "private. Log into an account that has access. Use --cookies, --cookies-from-browser, " +
                "--username and --password, --netrc-cmd, or --netrc (tiktok) to provide account credentials. " +
                "See  https://github.com/yt-dlp/yt-dlp/wiki/FAQ#how-do-i-pass-cookies-to-yt-dlp  for how to " +
                "manually pass cookies",
        ),
    )

    @Test
    fun genuineExtractorBugStillSuggestsAnUpdate() = assertEquals(
        "TikTok did not return this post. ${FriendlyError.UPDATE_HINT}",
        tiktok(
            "ERROR: [TikTok] 1: Unable to extract universal data for rehydration; please report this issue " +
                "on  https://github.com/yt-dlp/yt-dlp/issues?q= , filling out the appropriate issue " +
                "template. Confirm you are on the latest version using  yt-dlp -U",
        ),
    )
}
