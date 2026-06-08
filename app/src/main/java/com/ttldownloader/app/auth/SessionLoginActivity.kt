package com.ttldownloader.app.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.ttldownloader.app.TtlApp
import com.ttldownloader.app.net.Platform
import kotlinx.coroutines.launch

/**
 * Full-screen WebView that signs the user into TikTok or Instagram, captures the
 * `sessionid` cookie once login succeeds, and hands it to the backend (which uses it
 * to download private/age-restricted media). Returns RESULT_OK on success.
 *
 * Why a WebView: the backend's own "guided browser login" is Windows-only. On a phone
 * the cookie must be captured client-side after the user authenticates.
 */
class SessionLoginActivity : ComponentActivity() {

    private lateinit var platform: Platform
    private var captured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        platform = runCatching { Platform.valueOf(intent.getStringExtra(EXTRA_PLATFORM)!!) }
            .getOrDefault(Platform.INSTAGRAM)
        title = "Log in to ${platform.label}"

        val webView = WebView(this)
        setContentView(webView)

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(webView, true)
        // Start from a clean slate so we capture a real, fresh login session.
        cookies.removeAllCookies(null)
        cookies.flush()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // A non-WebView user agent — Instagram/TikTok block the default "; wv" UA.
            userAgentString = MOBILE_UA
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (captured) return
                val header = cookies.getCookie(cookieUrl(platform)) ?: return
                val sessionId = extractCookie(header, "sessionid")
                if (sessionId != null && sessionId.length > 8) {
                    captured = true
                    saveAndFinish(sessionId)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finishCancelled()
        }

        webView.loadUrl(loginUrl(platform))
    }

    private fun saveAndFinish(sessionId: String) {
        lifecycleScope.launch {
            val app = applicationContext as TtlApp
            val result = runCatching { app.api.saveSession(platform, sessionId) }
            if (result.isSuccess) {
                Toast.makeText(this@SessionLoginActivity, "Signed in to ${platform.label}", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
            } else {
                Toast.makeText(
                    this@SessionLoginActivity,
                    "Captured login but couldn't reach the backend.",
                    Toast.LENGTH_LONG,
                ).show()
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    private fun finishCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val EXTRA_PLATFORM = "platform"
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        fun intent(context: Context, platform: Platform): Intent =
            Intent(context, SessionLoginActivity::class.java).putExtra(EXTRA_PLATFORM, platform.name)

        private fun loginUrl(platform: Platform): String = when (platform) {
            Platform.TIKTOK -> "https://www.tiktok.com/login"
            Platform.INSTAGRAM -> "https://www.instagram.com/accounts/login/"
        }

        private fun cookieUrl(platform: Platform): String = when (platform) {
            Platform.TIKTOK -> "https://www.tiktok.com"
            Platform.INSTAGRAM -> "https://www.instagram.com"
        }

        /** Pull a single cookie value out of a "a=1; b=2" cookie header. */
        private fun extractCookie(header: String, name: String): String? =
            header.split(";")
                .map { it.trim() }
                .firstOrNull { it.startsWith("$name=") }
                ?.substringAfter("=")
                ?.takeIf { it.isNotBlank() }
    }
}
