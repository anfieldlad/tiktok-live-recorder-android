package com.ttldownloader.app.auth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.ttldownloader.app.TtlApp
import com.ttldownloader.app.net.Platform
import kotlinx.coroutines.launch

/**
 * Full-screen WebView that signs the user into TikTok or Instagram, captures the
 * `sessionid` cookie once login succeeds, and hands it to the backend.
 *
 * After a Google/third-party login the platform often redirects via JS to a page that
 * renders blank in a WebView and never fires onPageFinished — so relying on that alone
 * misses the cookie. Instead we POLL the cookie store (the sessionid is set the moment
 * login completes), and also offer an "I'm logged in" button as a manual fallback.
 */
class SessionLoginActivity : ComponentActivity() {

    private lateinit var platform: Platform
    private var captured = false
    private val handler = Handler(Looper.getMainLooper())

    private val pollCookies = object : Runnable {
        override fun run() {
            if (captured) return
            val sessionId = currentSessionId()
            if (sessionId != null) {
                captured = true
                saveAndFinish(sessionId)
            } else {
                handler.postDelayed(this, 1500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        platform = runCatching { Platform.valueOf(intent.getStringExtra(EXTRA_PLATFORM)!!) }
            .getOrDefault(Platform.INSTAGRAM)
        title = "Log in to ${platform.label}"

        val webView = WebView(this)
        setContentView(buildLayout(webView))

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(webView, true)
        // Start from a clean slate so we capture a real, fresh login session.
        cookies.removeAllCookies(null)
        cookies.flush()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // A non-WebView user agent — TikTok/Instagram/Google block the default "; wv" UA.
            userAgentString = MOBILE_UA
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (!captured) currentSessionId()?.let { captured = true; saveAndFinish(it) }
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finishCancelled()
        }

        webView.loadUrl(loginUrl(platform))
        // Begin polling for the session cookie shortly after the page starts loading.
        handler.postDelayed(pollCookies, 2500)
    }

    /** Top bar with a manual "I'm logged in" capture button, above the WebView. */
    private fun buildLayout(webView: WebView): LinearLayout {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#15151D"))
            setPadding(dp(16), dp(10), dp(12), dp(10))
        }
        val title = TextView(this).apply {
            text = "Log in to ${platform.label}"
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val done = Button(this).apply {
            text = "I'm logged in"
            setOnClickListener { captureNow() }
        }
        bar.addView(title)
        bar.addView(done)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    /** Read the current sessionid from the cookie store, or null if not logged in yet. */
    private fun currentSessionId(): String? {
        val cookies = CookieManager.getInstance()
        cookies.flush()
        val header = cookies.getCookie(cookieUrl(platform)) ?: return null
        return extractCookie(header, "sessionid")
    }

    private fun captureNow() {
        if (captured) return
        val sessionId = currentSessionId()
        if (sessionId != null) {
            captured = true
            saveAndFinish(sessionId)
        } else {
            Toast.makeText(this, "Not signed in yet — finish logging in first.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndFinish(sessionId: String) {
        handler.removeCallbacks(pollCookies)
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

    override fun onDestroy() {
        handler.removeCallbacks(pollCookies)
        super.onDestroy()
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
                ?.takeIf { it.length > 8 }
    }
}
