package com.stillhere.app.ui.ledger

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Every screen is built from these, so they are tested directly.
 *
 * Instrumented rather than JVM tests because `createComposeRule` needs a
 * device. They compile on every build via `assembleDebugAndroidTest`; run them
 * with `./gradlew :app:connectedDebugAndroidTest` when a device is attached.
 */
class LedgerPrimitivesTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sheet_renders_its_content() {
        compose.setContent { LedgerTheme { Sheet { Text("Record a broadcast") } } }
        compose.onNodeWithText("Record a broadcast").assertIsDisplayed()
    }

    @Test
    fun entry_card_shows_its_register_number_and_stamp() {
        compose.setContent {
            LedgerTheme {
                EntryCard(register = "No. 3FC629", stamp = { Stamp("Filed", StampKind.Filed) }) {
                    Text("tiktok.com/@someone/video/1")
                }
            }
        }
        compose.onNodeWithText("No. 3FC629").assertIsDisplayed()
        compose.onNodeWithText("FILED").assertIsDisplayed()
        compose.onNodeWithText("tiktok.com/@someone/video/1").assertIsDisplayed()
    }

    @Test
    fun ledger_field_shows_its_label_and_placeholder() {
        compose.setContent {
            LedgerTheme {
                LedgerField(
                    label = "Link — TikTok or Instagram",
                    value = "",
                    onValueChange = {},
                    placeholder = "tiktok.com/@…",
                )
            }
        }
        compose.onNodeWithText("LINK — TIKTOK OR INSTAGRAM").assertIsDisplayed()
        compose.onNodeWithText("tiktok.com/@…").assertIsDisplayed()
    }

    @Test
    fun ledger_button_click_and_disabled_state() {
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                LedgerButton(text = "Begin capture", onClick = { clicks++ })
                LedgerButton(text = "Place the order", onClick = { }, enabled = false)
            }
        }
        compose.onNodeWithText("BEGIN CAPTURE").assertIsEnabled().performClick()
        compose.onNodeWithText("PLACE THE ORDER").assertIsNotEnabled()
        assertEquals(1, clicks)
    }

    @Test
    fun eyebrow_and_filed_head_render_their_labels() {
        compose.setContent {
            LedgerTheme {
                Eyebrow("Entry — saved post")
                FiledHead()
            }
        }
        compose.onNodeWithText("ENTRY — SAVED POST").assertIsDisplayed()
        compose.onNodeWithText("Filed").assertIsDisplayed()
    }

    @Test
    fun empty_state_says_what_is_missing() {
        compose.setContent {
            LedgerTheme { LedgerEmpty("Nothing filed yet", "Paste a link above.") }
        }
        compose.onNodeWithText("Nothing filed yet").assertIsDisplayed()
        compose.onNodeWithText("Paste a link above.").assertIsDisplayed()
    }

    @Test
    fun every_stamp_kind_renders() {
        compose.setContent {
            LedgerTheme {
                StampKind.entries.forEach { Stamp(it.name, it) }
            }
        }
        StampKind.entries.forEach {
            compose.onNodeWithText(it.name.uppercase()).assertIsDisplayed()
        }
    }
}
