package net.tenbo.hubble.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The MSN Messenger skin for the desktop client — Luna-era blue gradients, silvery
 * buddy-list panels, status dots, and Tahoma-ish sans. Deliberately nostalgic; lives
 * only on desktop (the phone keeps the editorial look).
 */
object Msn {
    val TitleTop = Color(0xFF3C8DDC)
    val TitleBottom = Color(0xFF15599E)
    val PanelTop = Color(0xFFEAF1FB)
    val PanelBottom = Color(0xFFCBDDF4)
    val Content = Color(0xFFFFFFFF)
    val Hover = Color(0xFFCFE2FA)
    val InkDark = Color(0xFF13314F)
    val Subtle = Color(0xFF6A8099)
    val Green = Color(0xFF5BB81E)   // online / the butterfly
    val Amber = Color(0xFFEFA21E)   // away
    val Red = Color(0xFFD23A2C)     // busy
    val Gray = Color(0xFF9AA6B2)    // offline
    val Border = Color(0xFF9DBBE0)

    val sans = FontFamily.SansSerif

    val titleBrush get() = Brush.verticalGradient(listOf(TitleTop, TitleBottom))
    val panelBrush get() = Brush.verticalGradient(listOf(PanelTop, PanelBottom))
}

enum class Presence(val color: Color, val label: String) {
    ONLINE(Msn.Green, "Online"),
    AWAY(Msn.Amber, "Away"),
    BUSY(Msn.Red, "Busy"),
    OFFLINE(Msn.Gray, "Appear offline"),
}

@Composable
fun StatusDot(presence: Presence, dp: Int = 10) {
    Box(Modifier.size(dp.dp).clip(RoundedCornerShape(50)).background(presence.color))
}

@Composable
fun msnTitle(text: String) =
    Text(text, color = Color.White, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 13.sp)

@Composable
fun GradientHeader(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Msn.titleBrush).padding(horizontal = 12.dp, vertical = 10.dp)) {
        content()
    }
}

@Composable
fun MsnButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F8FE), Color(0xFFD6E5F8))))
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) { Text(text, color = Msn.InkDark, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}
