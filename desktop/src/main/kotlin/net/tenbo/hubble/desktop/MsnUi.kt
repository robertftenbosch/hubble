package net.tenbo.hubble.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The buddy list — your status header + matches as MSN contacts; double-click to chat. */
@Composable
fun MsnContactList(client: HubbleClient) {
    Column(Modifier.fillMaxSize().background(Msn.panelBrush)) {
        GradientHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Msn.Green), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text("✦", color = Color.White, fontSize = 20.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    androidx.compose.material3.Text(
                        "you (${client.identity?.hubbleId?.take(8) ?: ""})",
                        color = Color.White, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    )
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickableNoRipple { menuOpen = true },
                        ) {
                            StatusDot(client.presence, 8)
                            Spacer(Modifier.width(6.dp))
                            androidx.compose.material3.Text(
                                "${client.presence.label}  ▾", color = Color(0xFFD9E8FF), fontFamily = Msn.sans, fontSize = 11.sp,
                            )
                        }
                        androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            Presence.entries.forEach { p ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StatusDot(p, 8); Spacer(Modifier.width(8.dp))
                                            androidx.compose.material3.Text(p.label, fontFamily = Msn.sans, fontSize = 12.sp, color = Msn.InkDark)
                                        }
                                    },
                                    onClick = { client.changePresence(p); menuOpen = false },
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            androidx.compose.material3.Text(
                "Matches (${client.matches.size})",
                color = Msn.InkDark, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            if (client.matches.isEmpty()) {
                androidx.compose.material3.Text(
                    "No one here yet. On your phone, tap 'Sync to my computer' so your matches sign in here.",
                    color = Msn.Subtle, fontFamily = Msn.sans, fontSize = 11.sp,
                )
            }
            client.matches.forEach { m ->
                Row(
                    Modifier.fillMaxWidth()
                        .pointerInput(m.hubbleId) { detectTapGestures(onDoubleTap = { client.openChat(m) }, onTap = { client.openChat(m) }) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(Presence.ONLINE)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        androidx.compose.material3.Text(m.name, color = Msn.InkDark, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        androidx.compose.material3.Text("♪ ${m.place}", color = Msn.Subtle, fontFamily = Msn.sans, fontStyle = FontStyle.Italic, fontSize = 11.sp)
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Msn.Border))
        Row(Modifier.fillMaxWidth().background(Msn.PanelTop).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MsnButton(if (client.soundOn) "Sounds: On" else "Sounds: Off") { client.toggleSound() }
            MsnButton("Tonight") { client.openMap() }
            MsnButton("Sync") { client.sync() }
            MsnButton("Sign out") { client.signOut() }
        }
    }
}

/** Classic MSN chat window — blue title bar, "X says:" history, toolbar, input + Send/Nudge. */
/** Classic MSN emoticons: turn text shortcuts into smileys. */
private fun emotify(s: String): String = s
    .replace(":)", "🙂").replace(":-)", "🙂")
    .replace(":(", "🙁").replace(":-(", "🙁")
    .replace(":D", "😄").replace(";)", "😉")
    .replace(":P", "😛").replace(":p", "😛")
    .replace("(Y)", "👍").replace("(N)", "👎")
    .replace("(L)", "❤️").replace("(H)", "😎")
    .replace("(6)", "😈").replace(":'(", "😢")

@Composable
fun MsnChat(client: HubbleClient) {
    val m = client.current ?: return
    var draft by remember { mutableStateOf("") }
    // Nudge shake: damped left/right wobble of the whole window content.
    val shakeX = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(client.nudge) {
        if (client.nudge == 0) return@LaunchedEffect
        repeat(6) { i -> shakeX.animateTo(if (i % 2 == 0) 16f else -16f, androidx.compose.animation.core.tween(45)) }
        shakeX.animateTo(0f, androidx.compose.animation.core.tween(45))
    }
    Column(
        Modifier.fillMaxSize().background(Msn.Content)
            .offset { androidx.compose.ui.unit.IntOffset(shakeX.value.toInt(), 0) },
    ) {
        GradientHeader {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                msnTitle(m.name)
                androidx.compose.material3.Text(
                    "Contacts ✕", color = Color(0xFFD9E8FF), fontFamily = Msn.sans, fontSize = 11.sp,
                    modifier = Modifier.clickableNoRipple { client.closeChat() },
                )
            }
        }
        androidx.compose.material3.Text(
            "To: ${m.name} <online>", color = Msn.Subtle, fontFamily = Msn.sans, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Msn.Border))

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
            client.messages.forEach { line ->
                val who = if (line.fromMe) "You" else m.name
                if (line.text == NUDGE_MARKER) {
                    androidx.compose.material3.Text(
                        "📳 ${if (line.fromMe) "You" else m.name} sent a nudge!",
                        color = Msn.Subtle, fontFamily = Msn.sans, fontStyle = FontStyle.Italic, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                } else {
                    val color = if (line.fromMe) Msn.TitleBottom else Color(0xFFB5267A)
                    androidx.compose.material3.Text("$who says:", color = color, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    androidx.compose.material3.Text(emotify(line.text), color = Msn.InkDark, fontFamily = Msn.sans, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp, bottom = 8.dp))
                }
            }
        }

        // "is typing…" status line (MSN's bottom hint), shown when a ping recently arrived.
        if (client.typingFrom == m.hubbleId) {
            androidx.compose.material3.Text(
                "${m.name} is typing a message…",
                color = Msn.Subtle, fontFamily = Msn.sans, fontStyle = FontStyle.Italic, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Msn.Border))
        // Faux formatting toolbar
        Row(Modifier.fillMaxWidth().background(Msn.PanelTop).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Text("A", color = Msn.InkDark, fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(14.dp))
            androidx.compose.material3.Text("☺", color = Msn.Amber, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            MsnButton("Nudge") { client.sendNudge() }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(Color.White)
                    .border(1.dp, Msn.Border, RoundedCornerShape(4.dp)).padding(10.dp),
            ) {
                BasicTextField(
                    value = draft, onValueChange = { draft = it; client.notifyTyping() },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Msn.InkDark, fontFamily = Msn.sans, fontSize = 13.sp),
                    cursorBrush = SolidColor(Msn.TitleBottom), modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) androidx.compose.material3.Text("Type a message…", color = Msn.Subtle, fontFamily = Msn.sans, fontSize = 13.sp)
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            MsnButton("Send") { if (draft.isNotBlank()) { client.send(draft); draft = "" } }
        }
    }
}

/** Contents of the corner toast window. */
@Composable
fun MsnToast(toast: Toast, onOpen: () -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Msn.panelBrush).border(1.dp, Msn.TitleBottom)
            .clickableNoRipple(onOpen),
    ) {
        GradientHeader {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text("✦", color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    msnTitle("Hubble")
                }
                androidx.compose.material3.Text("✕", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickableNoRipple(onClose))
            }
        }
        Column(Modifier.padding(12.dp)) {
            androidx.compose.material3.Text("${toast.name} says:", color = Color(0xFFB5267A), fontFamily = Msn.sans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.Text(
                toast.text, color = Msn.InkDark, fontFamily = Msn.sans, fontSize = 13.sp,
                maxLines = 2,
            )
        }
    }
}
