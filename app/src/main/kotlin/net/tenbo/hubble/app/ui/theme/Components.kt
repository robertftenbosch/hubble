package net.tenbo.hubble.app.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** Clickable without the Material ripple — keeps the editorial surfaces calm. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** The wordmark: lowercase mono with a signal dot — a small beacon. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("hubble", style = MonoLabel.copy(fontSize = 15.sp, letterSpacing = 2.sp), color = Ink)
        Box(
            Modifier.padding(start = 6.dp).size(5.dp).clip(RoundedCornerShape(50)).background(Signal),
        )
    }
}

/** A mono uppercase label that sits above content like a dateline. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Ash) {
    Text(text.uppercase(), style = MonoLabel, color = color, modifier = modifier)
}

/**
 * The signature: a coordinate stamp marking where/when you crossed paths.
 * e.g. "CAFÉ LÉON · TUE 18:40 · ×2". Structure that encodes real encounter data.
 */
@Composable
fun CoordinateStamp(text: String, modifier: Modifier = Modifier) {
    Eyebrow("◦ $text", modifier = modifier, color = Signal)
}

/** A hairline rule — the only divider in the system. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Line))
}

/** Primary action: a filled signal-blue block with a mono label. */
@Composable
fun SignalButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (enabled) Signal else PaperSunk
    val fg = if (enabled) Paper else Ash
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = MonoLabel.copy(fontSize = 13.sp), color = fg)
    }
}

/** Secondary action: a quiet text button in ink. */
@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).clickableNoRipple(onClick).padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = MonoLabel.copy(fontSize = 13.sp), color = Ink)
    }
}

/** A small circular avatar for lists — photo, or the person's initial. */
@Composable
fun Avatar(path: String?, fallbackInitial: String, modifier: Modifier = Modifier, sizeDp: Int = 56) {
    val bitmap = remember(path) {
        path?.takeIf { File(it).exists() }?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Box(
        modifier.size(sizeDp.dp).clip(RoundedCornerShape(50)).background(PaperSunk).border(1.dp, Line, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape(50)),
            )
        } else {
            Text(fallbackInitial.take(1).uppercase(), fontFamily = Spectral, fontWeight = FontWeight.Medium, fontSize = (sizeDp / 2.4).sp, color = Ash)
        }
    }
}

/**
 * A profile photo plate: a tall rounded rectangle. If no photo is set, a quiet
 * placeholder with the person's initial. Photos are loaded from a local file path.
 */
@Composable
fun ProfilePhoto(path: String?, fallbackInitial: String, modifier: Modifier = Modifier) {
    val bitmap = remember(path) {
        path?.takeIf { File(it).exists() }?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(10.dp))
            .background(PaperSunk)
            .border(1.dp, Line, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Text(
                fallbackInitial.take(1).uppercase(),
                fontWeight = FontWeight.Medium,
                fontSize = 64.sp,
                color = Ash,
                fontFamily = Spectral,
                textAlign = TextAlign.Center,
            )
        }
    }
}
