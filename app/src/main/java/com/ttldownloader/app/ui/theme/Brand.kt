package com.ttldownloader.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttldownloader.app.net.Platform

/** The signature violet→pink brand gradient used on primary surfaces and buttons. */
val BrandGradient = Brush.linearGradient(listOf(Color(0xFF8A5CFF), Color(0xFFFF3B6B)))

/** A soft top-of-screen glow layered over the dark background. */
val HeroGlow = Brush.verticalGradient(listOf(Color(0xFF241A3D), Color(0xFF0B0B10)))

private val DisabledFill = SolidColor(Color(0xFF24242F))
private val DisabledText = Color(0xFF6A6A78)

/** Per-platform accent gradient — TikTok cyan→red, Instagram sunset. */
fun platformBrush(platform: Platform): Brush = when (platform) {
    Platform.TIKTOK -> Brush.linearGradient(listOf(Color(0xFF25F4EE), Color(0xFFFE2C55)))
    Platform.INSTAGRAM -> Brush.linearGradient(listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF)))
}

/** A single representative accent color for a platform (chips, dots). */
fun platformColor(platform: Platform): Color = when (platform) {
    Platform.TIKTOK -> Color(0xFFFE2C55)
    Platform.INSTAGRAM -> Color(0xFFDD2A7B)
}

/** Full-width primary action with the brand gradient; greys out cleanly when disabled. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(if (enabled) BrandGradient else DisabledFill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else DisabledText,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = if (enabled) Color.White else DisabledText,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Circular gradient avatar with the platform's initial — used as a list/leading badge. */
@Composable
fun PlatformBadge(platform: Platform, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(platformBrush(platform)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            platform.label.first().toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Small pill showing the platform, with a leading dot in the platform color. */
@Composable
fun PlatformChip(platform: Platform) {
    val color = platformColor(platform)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(platform.label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/** App identity badge — rounded gradient square with a download glyph. */
@Composable
fun AppLogoBadge(icon: ImageVector, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}
