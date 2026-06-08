package com.ttldownloader.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark-first brand palette — a downloader app reads best on a near-black canvas with a
// violet→pink accent that nods to both TikTok and Instagram without copying either.
private val BrandColors = darkColorScheme(
    primary = Color(0xFFFF3B6B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8A5CFF),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0B0B10),
    onBackground = Color(0xFFEDEDF2),
    surface = Color(0xFF15151D),
    onSurface = Color(0xFFEDEDF2),
    surfaceVariant = Color(0xFF1F1F2A),
    onSurfaceVariant = Color(0xFFB6B6C2),
    outline = Color(0xFF3A3A47),
    error = Color(0xFFFF5A5F),
    onError = Color(0xFFFFFFFF),
)

private val BrandShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TtlTheme(content: @Composable () -> Unit) {
    // Always dark — the brand identity is consistent regardless of the system setting.
    MaterialTheme(
        colorScheme = BrandColors,
        shapes = BrandShapes,
        typography = Typography(),
        content = content,
    )
}
