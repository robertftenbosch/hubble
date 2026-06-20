package net.tenbo.hubble.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Same editorial palette as the Android app.
val Paper = Color(0xFFF4F5F3)
val PaperSunk = Color(0xFFEAEBE6)
val Ink = Color(0xFF15171C)
val Ash = Color(0xFF6B6F76)
val Line = Color(0xFFE0E1DB)
val Signal = Color(0xFF2436E8)

// Fonts loaded from the desktop classpath (src/main/resources/font/*).
val Spectral = FontFamily(
    Font("font/spectral_regular.ttf", FontWeight.Normal),
    Font("font/spectral_medium.ttf", FontWeight.Medium),
    Font("font/spectral_semibold.ttf", FontWeight.SemiBold),
)
val PlexMono = FontFamily(
    Font("font/ibm_plex_mono_regular.ttf", FontWeight.Normal),
    Font("font/ibm_plex_mono_medium.ttf", FontWeight.Medium),
)

val MonoLabel = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.5.sp)

private val typography = Typography(
    displayMedium = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = MonoLabel,
)

private val colors = lightColorScheme(
    primary = Signal, onPrimary = Paper, background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink, surfaceVariant = PaperSunk, onSurfaceVariant = Ash, outline = Line,
)

@Composable
fun HubbleTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = colors, typography = typography, content = content)

// ── Components (mirrors the Android design system) ──────────────────────────────
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
}

@Composable
fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("hubble", style = MonoLabel.copy(fontSize = 15.sp, letterSpacing = 2.sp), color = Ink)
        Box(Modifier.padding(start = 6.dp).size(5.dp).clip(RoundedCornerShape(50)).background(Signal))
    }
}

@Composable
fun Eyebrow(text: String, color: Color = Ash) = Text(text.uppercase(), style = MonoLabel, color = color)

@Composable
fun CoordinateStamp(text: String) = Eyebrow("◦ $text", color = Signal)

@Composable
fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(Line))

@Composable
fun SignalButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (enabled) Signal else PaperSunk
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = MonoLabel.copy(fontSize = 13.sp), color = if (enabled) Paper else Ash) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).clickableNoRipple(onClick).padding(vertical = 12.dp, horizontal = 8.dp)) {
        Text(text.uppercase(), style = MonoLabel.copy(fontSize = 13.sp), color = Ink)
    }
}

@Composable
fun Avatar(initial: String, sizeDp: Int = 48) {
    Box(
        Modifier.size(sizeDp.dp).clip(RoundedCornerShape(50)).background(PaperSunk).border(1.dp, Line, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) { Text(initial.take(1).uppercase(), fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = (sizeDp / 2.4).sp, color = Ash) }
}
