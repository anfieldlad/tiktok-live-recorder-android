package com.ttldownloader.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.ttldownloader.app.domain.UrlRouter

/**
 * Invisible entry point registered for ACTION_SEND so the app appears in TikTok's and
 * Instagram's "Share" menu. It pulls the URL out of the shared text, kicks off the
 * download (in the foreground service), then finishes immediately — no UI of its own.
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        val routed = UrlRouter.route(sharedText)
        if (routed == null) {
            Toast.makeText(this, "No TikTok or Instagram link found in that share.", Toast.LENGTH_LONG).show()
        } else {
            val app = applicationContext as TtlApp
            app.controller.clear()
            app.controller.enqueue(app, sharedText!!)
            Toast.makeText(this, "Downloading ${routed.platform.label}…", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
