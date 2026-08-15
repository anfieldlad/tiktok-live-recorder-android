package com.stillhere.app.ui.ledger

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Three faces, matching the web: Fraunces for display, Newsreader for body,
 * Cutive Mono for labels and stamps.
 *
 * The families currently resolve to the platform's serif and monospace. Every
 * size, weight and letter-spacing is already the web's, so the app is shippable
 * and correct in shape before the OFL files are dropped in, and gains the real
 * faces the moment they are — see `docs/fonts.md`, a four-line change in this
 * file and nowhere else.
 *
 * They are bundled rather than fetched through the Google Fonts provider so
 * there is no Play Services dependency, it works offline, and there is no flash
 * of fallback type on a cold start.
 */
object LedgerType {
    val display: FontFamily = FontFamily.Serif
    val body: FontFamily = FontFamily.Serif
    val mono: FontFamily = FontFamily.Monospace

    /** Mono, uppercase, widely tracked — the register's label voice. */
    val label = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Normal,
    )

    /** The stamp face: tighter than a label, always uppercase at the call site. */
    val stamp = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Normal,
    )

    /**
     * The nav strip: the label face with tracking eased off.
     *
     * Three labels share the width of a phone, and tracking is what gives way
     * — never the type size, which is the thing that has to stay legible.
     */
    /**
     * The eyebrow: the longest mono string on any screen
     * ("Entry — standing order · TikTok"), so it is tracked tighter than a
     * label to survive a 320dp phone on one line.
     */
    val eyebrow = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.Normal,
    )

    val navLabel = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
        fontWeight = FontWeight.Normal,
    )
}

val LedgerTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = LedgerType.display,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontWeight = FontWeight.Normal,
    ),
    headlineMedium = TextStyle(
        fontFamily = LedgerType.display,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        fontWeight = FontWeight.Normal,
    ),
    titleMedium = TextStyle(
        fontFamily = LedgerType.display,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyLarge = TextStyle(
        fontFamily = LedgerType.body,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = LedgerType.body,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = LedgerType.label,
    labelSmall = LedgerType.stamp,
)
