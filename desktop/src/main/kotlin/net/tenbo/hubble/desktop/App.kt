package net.tenbo.hubble.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tenbo.hubble.core.message.MatchSnapshot

@Composable
fun App(client: HubbleClient) {
    HubbleTheme {
        Surface(Modifier.fillMaxSize()) {
            when {
                client.identity == null -> RestoreScreen(client)
                client.showMap -> MapView(client)
                client.current != null -> MsnChat(client)   // MSN-style chat window
                else -> MsnContactList(client)               // MSN-style buddy list
            }
        }
    }
}

@Composable
private fun RestoreScreen(client: HubbleClient) {
    var phrase by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center) {
        Wordmark()
        Spacer(Modifier.height(40.dp))
        Eyebrow("Same you, bigger screen")
        Spacer(Modifier.height(16.dp))
        Text("Sign in on this computer", style = MaterialTheme.typography.displayMedium, color = Ink)
        Spacer(Modifier.height(16.dp))

        val pairingQr = client.pairingQr
        if (pairingQr != null) {
            // Pair-from-phone mode: show the QR, no typing needed.
            Text(
                "Open Hubble on your phone, tap You → Pair desktop, and scan this code.",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.width(280.dp).background(Paper).border(1.dp, Line).padding(12.dp),
            ) {
                PairingQrCanvas(pairingQr, Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Spacer(Modifier.height(14.dp))
            Eyebrow(client.pairingStatus)
            Spacer(Modifier.height(20.dp))
            GhostButton("Cancel") { client.cancelPairing() }
        } else {
            // Fallback: type the recovery phrase.
            Text(
                "Enter your twelve-word recovery phrase from the Hubble app on your phone, " +
                    "or pair without typing by scanning a QR with the app.",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PaperSunk)
                    .border(1.dp, Line, RoundedCornerShape(10.dp)).padding(18.dp),
            ) {
                BasicTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    textStyle = MonoLabel.copy(fontSize = 15.sp, letterSpacing = 0.5.sp, color = Ink),
                    cursorBrush = SolidColor(Signal),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (phrase.isEmpty()) Text("abandon ability able …", style = MonoLabel.copy(fontSize = 15.sp), color = Ash)
                        inner()
                    },
                )
            }
            client.restoreError?.let {
                Spacer(Modifier.height(10.dp)); Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SignalButton("Restore my identity", enabled = phrase.isNotBlank()) { client.restore(phrase) }
                GhostButton("Pair from phone") { client.startPairing() }
            }
        }
    }
}

@Composable
private fun HomeScreen(client: HubbleClient) {
    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Wordmark()
            Row {
                GhostButton("Tonight") { client.openMap() }
                GhostButton("Sync") { client.sync() }
                GhostButton("Sign out") { client.signOut() }
            }
        }
        Spacer(Modifier.height(28.dp))
        Eyebrow(client.status)
        Spacer(Modifier.height(20.dp))
        Text("Matches", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(20.dp))

        if (client.matches.isEmpty()) {
            Text(
                "No matches here yet. On your phone, open a match and your conversations will " +
                    "sync to this computer. (New people are always met in person, on the phone.)",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                client.matches.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { client.openChat(m) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(m.name)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("${m.name}, ${m.age}", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Spacer(Modifier.height(4.dp))
                            Eyebrow("matched · ${m.place}")
                        }
                    }
                    Rule()
                }
            }
        }
    }
}

@Composable
private fun MapView(client: HubbleClient) {
    val checkins = listOf("Amsterdam" to "u173zw", "Amsterdam East" to "u173zt", "Haarlem" to "u15g8x")
    Column(Modifier.fillMaxSize().padding(40.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Wordmark(); GhostButton("Back") { client.closeMap() }
        }
        Spacer(Modifier.height(28.dp))
        Eyebrow("Tonight")
        Spacer(Modifier.height(16.dp))
        Text("Where it's alive", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(20.dp))

        if (client.heat.isEmpty()) {
            Text("Quiet out there right now.", style = MaterialTheme.typography.bodyLarge, color = Ash)
        } else {
            val points = client.heat.map { it to Geohash.decode(it.cell) }
            val minLat = points.minOf { it.second.first }; val maxLat = points.maxOf { it.second.first }
            val minLon = points.minOf { it.second.second }; val maxLon = points.maxOf { it.second.second }
            val maxCount = client.heat.maxOf { it.count }.toFloat()
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(12.dp)).background(PaperSunk)) {
                val pad = size.minDimension * 0.16f
                val w = size.width - pad * 2; val h = size.height - pad * 2
                fun norm(v: Double, lo: Double, hi: Double) = if (hi - lo < 1e-9) 0.5f else ((v - lo) / (hi - lo)).toFloat()
                points.forEach { (cell, ll) ->
                    val x = pad + norm(ll.second, minLon, maxLon) * w
                    val y = pad + (1f - norm(ll.first, minLat, maxLat)) * h
                    val core = size.minDimension * (0.018f + 0.030f * (cell.count / maxCount))
                    drawCircle(Signal.copy(alpha = 0.14f), core * 3.2f, Offset(x, y))
                    drawCircle(Signal.copy(alpha = 0.30f), core * 1.9f, Offset(x, y))
                    drawCircle(Signal, core, Offset(x, y))
                }
            }
            Spacer(Modifier.height(20.dp))
            Eyebrow("Areas alive")
            Spacer(Modifier.height(10.dp))
            client.heat.sortedByDescending { it.count }.forEach { c ->
                CoordinateStamp("${c.cell} · ${if (c.count >= 6) "buzzing" else if (c.count >= 4) "lively" else "a few about"}")
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
        Eyebrow("Check in")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            checkins.forEach { (label, cell) ->
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))
                        .clickableNoRipple { client.checkIn(cell) }.padding(horizontal = 14.dp, vertical = 10.dp),
                ) { Text(label, style = MonoLabel.copy(fontSize = 12.sp), color = Ink) }
            }
        }
    }
}

@Composable
private fun ChatScreen(client: HubbleClient) {
    val m = client.current ?: return
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            GhostButton("Back") { client.closeChat() }
            Spacer(Modifier.width(8.dp))
            Text(m.name, style = MaterialTheme.typography.headlineSmall, color = Ink)
        }
        Rule()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp)) {
            CoordinateStamp("matched · ${m.place}")
            Spacer(Modifier.height(16.dp))
            client.messages.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromMe) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (line.fromMe) Signal else PaperSunk)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) { Text(line.text, style = MaterialTheme.typography.bodyLarge, color = if (line.fromMe) Paper else Ink) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Rule()
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = draft, onValueChange = { draft = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    cursorBrush = SolidColor(Signal), modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> if (draft.isEmpty()) Text("Message", style = MaterialTheme.typography.bodyLarge, color = Ash); inner() },
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(if (draft.isBlank()) PaperSunk else Signal)
                    .clickableNoRipple { if (draft.isNotBlank()) { client.send(draft); draft = "" } }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) { Text("SEND", style = MonoLabel.copy(fontSize = 12.sp), color = if (draft.isBlank()) Ash else Paper) }
        }
    }
}
