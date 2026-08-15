package com.stillhere.app.ui.ledger

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stillhere.app.net.Platform

/**
 * The web app's token layer, in Kotlin.
 *
 * These are the same values as the custom properties in `app/static/css/app.css`
 * in the server repo. Building both products from a shared vocabulary — rather
 * than pixel-matching screens — is what keeps them selaras as they drift: when
 * the web changes a token, this object changes with it and every screen follows.
 */
object Ledger {
    /** The ruled board the paper sits on. */
    val Board = Color(0xFFEDE6D8)

    /** Cream card stock. */
    val Card = Color(0xFFF7F2E7)
    val CardEdge = Color(0xFFDCD2BE)

    val Ink = Color(0xFF23201B)
    // Tracks the web's --dim, which was darkened to clear WCAG AA on both
    // surfaces. #6E675C sat at exactly 4.5:1 on the board — passing with no
    // headroom; this is 5.45 / 6.07.
    val Dim = Color(0xFF635A4D)
    val Rule = Color(0xFFC9BFA8)

    /** Oxblood — the margin rule and the first platform's ink. */
    val SeriesInk = Color(0xFF7B2D26)

    /** The second ink, for Instagram entries. */
    val SeriesInkAlt = Color(0xFF3E5C4B)

    /** The three rubber stamps. */
    val Filed = Color(0xFF2F6B4F)
    val Pending = Color(0xFF8A7231)
    val FailedInk = Color(0xFF9B2C22)
}

/** Zero radius, everywhere. Declared once so it cannot drift per-component. */
val SquareCorners = RoundedCornerShape(0.dp)

val LedgerShapes = Shapes(
    extraSmall = SquareCorners,
    small = SquareCorners,
    medium = SquareCorners,
    large = SquareCorners,
    extraLarge = SquareCorners,
)

/** Hairline rules, matching the web's 1.5px. */
val RuleWidth = 1.5.dp

/** Which ink an entry is written in. */
fun seriesInk(platform: Platform): Color = when (platform) {
    Platform.TIKTOK -> Ledger.SeriesInk
    Platform.INSTAGRAM -> Ledger.SeriesInkAlt
}
