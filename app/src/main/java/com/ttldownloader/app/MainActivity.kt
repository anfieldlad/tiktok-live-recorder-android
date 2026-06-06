package com.ttldownloader.app

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ttldownloader.app.ui.AppRoot
import com.ttldownloader.app.ui.AppViewModel
import com.ttldownloader.app.ui.theme.TtlTheme

/**
 * Single Compose host. On every resume it checks the clipboard for a TikTok/Instagram link
 * and surfaces a one-tap banner (the supported Android pattern — clipboard reads are allowed
 * only while in the foreground). Also accepts ACTION_VIEW links opened into the app.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[AppViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TtlTheme {
                AppRoot(viewModel)
            }
        }
        handleViewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Re-check the clipboard each time we come to the foreground.
        viewModel.considerClipboard(readClipboardText())
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.let { viewModel.startDownload(it) }
        }
    }

    private fun readClipboardText(): String? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()
    }
}
