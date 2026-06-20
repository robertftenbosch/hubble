package net.tenbo.hubble.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.tenbo.hubble.app.R

// ── Palette ──────────────────────────────────────────────────────────────────
// Paper + ink + one signal. Deliberately cool/neutral (not cream) so the single
// beacon-blue accent carries all the warmth-as-intent.
val Paper = Color(0xFFF4F5F3)
val PaperSunk = Color(0xFFEAEBE6)
val Ink = Color(0xFF15171C)
val Ash = Color(0xFF6B6F76)
val Line = Color(0xFFE0E1DB)
val Signal = Color(0xFF2436E8) // the one accent: a proximity beacon

private val colors = lightColorScheme(
    primary = Signal,
    onPrimary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSunk,
    onSurfaceVariant = Ash,
    outline = Line,
    outlineVariant = Line,
    error = Color(0xFF8A1C1C),
)

// ── Type ─────────────────────────────────────────────────────────────────────
// Spectral: a literary serif for display — "real people, real stories".
// IBM Plex Mono: the utility face that carries the coordinate-stamp signature.
val Spectral = FontFamily(
    Font(R.font.spectral_regular, FontWeight.Normal),
    Font(R.font.spectral_medium, FontWeight.Medium),
    Font(R.font.spectral_semibold, FontWeight.SemiBold),
)
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/** The mono utility style used for eyebrows and coordinate stamps. */
val MonoLabel = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 1.5.sp,
)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = MonoLabel,
)

@Composable
fun HubbleTheme(content: @Composable () -> Unit) {
    // Light-only for now; the editorial palette is intentionally paper-first.
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
