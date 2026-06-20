package net.tenbo.hubble.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tenbo.hubble.app.data.FriendEntity
import net.tenbo.hubble.app.data.PostEntity
import net.tenbo.hubble.app.proximity.NearbyHubble
import net.tenbo.hubble.app.ui.theme.Ash
import net.tenbo.hubble.app.ui.theme.Avatar
import net.tenbo.hubble.app.ui.theme.Eyebrow
import net.tenbo.hubble.app.ui.theme.GhostButton
import net.tenbo.hubble.app.ui.theme.Ink
import net.tenbo.hubble.app.ui.theme.Line
import net.tenbo.hubble.app.ui.theme.MonoLabel
import net.tenbo.hubble.app.ui.theme.Paper
import net.tenbo.hubble.app.ui.theme.PaperSunk
import net.tenbo.hubble.app.ui.theme.Rule
import net.tenbo.hubble.app.ui.theme.Signal
import net.tenbo.hubble.app.ui.theme.SignalButton
import net.tenbo.hubble.app.ui.theme.Wordmark
import net.tenbo.hubble.app.ui.theme.clickableNoRipple
import net.tenbo.hubble.core.handshake.HandshakeState

@Composable
private fun screenScroll(): Modifier = Modifier
    .fillMaxSize()
    .verticalScroll(rememberScrollState())
    .padding(horizontal = 28.dp)
    .padding(top = 56.dp, bottom = 28.dp)

@Composable
private fun TopBar(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Wordmark()
        GhostButton(actionLabel, onClick = onAction)
    }
}

// ── Nearby (radar) ─────────────────────────────────────────────────────────────
/** People with Hubble open right now, by BLE proximity. Tap to connect in person. */
@Composable
fun NearbyScreen(nearby: List<NearbyHubble>, onTap: (NearbyHubble) -> Unit, onMap: () -> Unit, onBack: () -> Unit) {
    Column(screenScroll()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Wordmark()
            GhostButton("Map", onClick = onMap)
        }
        Spacer(Modifier.height(28.dp))
        Eyebrow("Right now")
        Spacer(Modifier.height(16.dp))
        Text("Around you", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(12.dp))

        if (nearby.isEmpty()) {
            Text(
                "Listening for people with Hubble open nearby. When someone's in range, " +
                    "they'll appear here — walk up and connect in person.",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
            Spacer(Modifier.height(20.dp))
            PulseDot()
        } else {
            Spacer(Modifier.height(4.dp))
            nearby.forEach { hub ->
                Row(
                    Modifier.fillMaxWidth().clickableNoRipple { onTap(hub) }.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("A hubble", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Eyebrow("${proximityLabel(hub.rssi)} · ${hub.address.takeLast(5)}")
                    }
                    Text("CONNECT", style = MonoLabel.copy(fontSize = 12.sp), color = Signal)
                }
                Rule()
            }
        }
    }
}

@Composable
private fun PulseDot() {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Signal))
}

private fun proximityLabel(rssi: Int): String = when {
    rssi > -55 -> "right here"
    rssi > -70 -> "close"
    else -> "nearby"
}

// ── In-person safety check (SAS) ────────────────────────────────────────────────
/** The in-person verification: both phones show the same five marks, or it's not safe. */
@Composable
fun SasConfirmScreen(state: HandshakeState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            is HandshakeState.AwaitingSasConfirmation -> {
                Eyebrow("Confirm in person")
                Spacer(Modifier.height(28.dp))
                Text(state.sasEmoji.joinToString("  "), fontSize = 52.sp)
                Spacer(Modifier.height(28.dp))
                Text(
                    "These five should match the other phone, right now, beside you. " +
                        "If they don't, someone's in the middle — don't confirm.",
                    style = MaterialTheme.typography.bodyMedium, color = Ash, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                SignalButton("They match", onClick = onConfirm)
                Spacer(Modifier.height(4.dp))
                GhostButton("Not a match", modifier = Modifier.fillMaxWidth(), onClick = onCancel)
            }
            is HandshakeState.Completed -> {
                Eyebrow("Connected")
                Spacer(Modifier.height(16.dp))
                Text("You met ${state.friend.displayName}", style = MaterialTheme.typography.headlineLarge, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                GhostButton("Done", onClick = onCancel)
            }
            is HandshakeState.Aborted -> {
                Eyebrow("Stopped")
                Spacer(Modifier.height(16.dp))
                Text("Couldn't verify safely.", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(state.reason, style = MaterialTheme.typography.bodyMedium, color = Ash, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                GhostButton("Back", onClick = onCancel)
            }
            else -> Text("Connecting…", style = MonoLabel, color = Ash)
        }
    }
}

// ── People you've met (legacy friends) ──────────────────────────────────────────
@Composable
fun FriendsScreen(friends: List<FriendEntity>, onBack: () -> Unit) {
    Column(screenScroll()) {
        TopBar("Met", "Back", onBack)
        Spacer(Modifier.height(28.dp))
        Eyebrow("People you've met")
        Spacer(Modifier.height(20.dp))
        if (friends.isEmpty()) {
            Text("Nobody yet. Connect with someone in person from Nearby.", style = MaterialTheme.typography.bodyLarge, color = Ash)
        } else {
            friends.forEach { f ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(null, f.displayName)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(f.displayName, style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Eyebrow("id ${f.hubbleId}")
                    }
                }
                Rule()
            }
        }
    }
}

// ── Stories feed ─────────────────────────────────────────────────────────────────
/** Ephemeral stories from people you've matched with. */
@Composable
fun FeedScreen(
    posts: List<PostEntity>,
    onCompose: () -> Unit,
    onNearby: () -> Unit,
    onFriends: () -> Unit,
    onProfile: () -> Unit = {},
) {
    Column(screenScroll()) {
        TopBar("Stories", "You", onProfile)
        Spacer(Modifier.height(28.dp))
        SignalButton("Share a moment", onClick = onCompose)
        Spacer(Modifier.height(24.dp))
        if (posts.isEmpty()) {
            Eyebrow("Nothing yet")
            Spacer(Modifier.height(12.dp))
            Text(
                "Stories from people you've matched with show up here for a day, then fade.",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
        } else {
            posts.forEach { post ->
                Column(Modifier.padding(vertical = 16.dp)) {
                    Eyebrow(if (post.mine) "${post.authorName} · you" else post.authorName)
                    Spacer(Modifier.height(8.dp))
                    Text(post.text, style = MaterialTheme.typography.headlineSmall, color = Ink)
                }
                Rule()
            }
        }
    }
}

/** Compose an ephemeral story (fades after 24h). */
@Composable
fun ComposeScreen(onPublish: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 56.dp, bottom = 28.dp)) {
        Eyebrow("Share a moment")
        Spacer(Modifier.height(16.dp))
        Text("What's happening?", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text("Visible to your matches for 24 hours, then gone.", style = MaterialTheme.typography.bodyMedium, color = Ash)
        Spacer(Modifier.height(24.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = MaterialTheme.typography.titleMedium.copy(color = Ink),
            cursorBrush = SolidColor(Signal),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.padding(bottom = 10.dp)) {
                    if (text.isEmpty()) Text("Say something…", style = MaterialTheme.typography.titleMedium, color = Ash)
                    inner()
                }
            },
        )
        Rule()
        Spacer(Modifier.weight(1f))
        SignalButton("Post", enabled = text.isNotBlank()) { onPublish(text) }
        Spacer(Modifier.height(4.dp))
        GhostButton("Cancel", modifier = Modifier.fillMaxWidth(), onClick = onCancel)
    }
}
