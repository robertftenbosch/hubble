package net.tenbo.hubble.app.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tenbo.hubble.app.data.MatchEntity
import net.tenbo.hubble.app.data.MessageEntity
import net.tenbo.hubble.app.data.Profile
import net.tenbo.hubble.app.ui.theme.Ash
import net.tenbo.hubble.app.ui.theme.Avatar
import net.tenbo.hubble.app.ui.theme.Paper
import net.tenbo.hubble.app.ui.theme.CoordinateStamp
import net.tenbo.hubble.app.ui.theme.Eyebrow
import net.tenbo.hubble.app.ui.theme.GhostButton
import net.tenbo.hubble.app.ui.theme.Ink
import net.tenbo.hubble.app.ui.theme.Line
import net.tenbo.hubble.app.ui.theme.MonoLabel
import net.tenbo.hubble.app.ui.theme.PaperSunk
import net.tenbo.hubble.app.ui.theme.ProfilePhoto
import net.tenbo.hubble.app.ui.theme.Rule
import net.tenbo.hubble.app.ui.theme.Signal
import net.tenbo.hubble.app.ui.theme.SignalButton
import net.tenbo.hubble.app.ui.theme.Wordmark
import net.tenbo.hubble.app.ui.theme.clickableNoRipple
import java.io.File

private enum class Step { WELCOME, KEY, PROFILE }

/**
 * First-run flow: a calm thesis, the recovery key, then building a profile.
 * On finish, [onDone] receives the assembled [Profile] (with the photo already
 * copied to internal storage). The recovery phrase is shown once to back up.
 */
@Composable
fun OnboardingFlow(recoveryPhrase: String, onDone: (Profile) -> Unit) {
    var step by remember { mutableStateOf(Step.WELCOME) }
    val context = LocalContext.current

    when (step) {
        Step.WELCOME -> WelcomeStep(onContinue = { step = Step.KEY })
        Step.KEY -> KeyStep(recoveryPhrase, onContinue = { step = Step.PROFILE })
        Step.PROFILE -> ProfileBuildStep(context, buttonLabel = "Enter Hubble", onDone = onDone)
    }
}

/** Edit an existing profile — the build step, prefilled. */
@Composable
fun ProfileEditScreen(initial: Profile, onSaved: (Profile) -> Unit) {
    ProfileBuildStep(LocalContext.current, initial = initial, buttonLabel = "Save", onDone = onSaved)
}

@Composable
private fun Screen(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 64.dp, bottom = 28.dp),
        content = content,
    )
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Screen {
        Wordmark()
        Spacer(Modifier.height(72.dp))
        Eyebrow("No profiles you'll never meet")
        Spacer(Modifier.height(20.dp))
        Text(
            "Dating that begins in the real world.",
            style = MaterialTheme.typography.displayMedium,
            color = Ink,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "You can only connect with people you've actually crossed paths with. " +
                "No bots, no catfish — everyone here was really there.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ash,
        )
        Spacer(Modifier.weight(1f))
        SignalButton("Begin", onClick = onContinue)
    }
}

@Composable
private fun KeyStep(phrase: String, onContinue: () -> Unit) {
    Screen {
        Eyebrow("Your only key")
        Spacer(Modifier.height(16.dp))
        Text("Keep this phrase safe", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(16.dp))
        Text(
            "Hubble has no account and no password. These twelve words are the only " +
                "way back to your identity if you lose your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ash,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(PaperSunk).border(1.dp, Line, RoundedCornerShape(10.dp))
                .padding(20.dp),
        ) {
            Text(phrase, style = MonoLabel.copy(letterSpacing = 0.5.sp, fontSize = 15.sp), color = Ink)
        }
        Spacer(Modifier.weight(1f))
        SignalButton("I've saved it", onClick = onContinue)
        Spacer(Modifier.height(4.dp))
        GhostButton("Skip for now", modifier = Modifier.fillMaxWidth(), onClick = onContinue)
    }
}

@Composable
private fun ProfileBuildStep(
    context: Context,
    buttonLabel: String,
    initial: Profile? = null,
    onDone: (Profile) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.age?.takeIf { it > 0 }?.toString() ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var bio by remember { mutableStateOf(initial?.bio ?: "") }
    var prompt by remember { mutableStateOf(initial?.promptAnswer ?: "") }
    var photoPath by remember { mutableStateOf(initial?.photoPath) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) photoPath = copyToInternal(context, uri)
    }

    val ageNum = age.toIntOrNull() ?: 0
    val valid = name.isNotBlank() && ageNum in 18..120

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 64.dp, bottom = 28.dp),
    ) {
        Eyebrow("Your profile")
        Spacer(Modifier.height(16.dp))
        Text("Who are you?", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(24.dp))

        Box(Modifier.clickableNoRipple {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            ProfilePhoto(photoPath, fallbackInitial = name.ifBlank { "+" })
        }
        Eyebrow(
            if (photoPath == null) "Tap to add a photo" else "Tap to change",
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(Modifier.height(28.dp))
        UnderlineField(name, { name = it }, "First name")
        Spacer(Modifier.height(20.dp))
        Row {
            UnderlineField(age, { age = it.filter(Char::isDigit).take(3) }, "Age", KeyboardType.Number, Modifier.width(96.dp))
            Spacer(Modifier.width(24.dp))
            UnderlineField(city, { city = it }, "City", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        UnderlineField(bio, { bio = it }, "A line about you")
        Spacer(Modifier.height(20.dp))
        Eyebrow(Profile.PROMPT_QUESTION)
        Spacer(Modifier.height(8.dp))
        UnderlineField(prompt, { prompt = it }, "e.g. the corner table at Café Léon")

        Spacer(Modifier.height(36.dp))
        SignalButton(buttonLabel, enabled = valid) {
            onDone(
                Profile(
                    name = name.trim(),
                    age = ageNum,
                    city = city.trim(),
                    bio = bio.trim(),
                    promptAnswer = prompt.trim(),
                    photoPath = photoPath,
                ),
            )
        }
    }
}

/** A profile in the editorial layout. [crossedPaths] drives the coordinate stamp. */
@Composable
fun ProfileScreen(profile: Profile, crossedPaths: Int, isSelf: Boolean, onEdit: () -> Unit, onStories: () -> Unit, onSyncDevices: () -> Unit, onPairDesktop: () -> Unit = {}, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 56.dp, bottom = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Wordmark()
            if (isSelf) {
                Row {
                    GhostButton("Stories", onClick = onStories)
                    GhostButton("Edit", onClick = onEdit)
                }
            } else {
                GhostButton("Back", onClick = onBack)
            }
        }
        Spacer(Modifier.height(40.dp))

        if (isSelf) Eyebrow("Your profile")
        else CoordinateStamp("crossed paths ×$crossedPaths")
        Spacer(Modifier.height(14.dp))

        Text(profile.name.ifBlank { "—" }, style = MaterialTheme.typography.displayLarge, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            listOfNotNull(profile.age.takeIf { it > 0 }?.toString(), profile.city.ifBlank { null })
                .joinToString("  ·  "),
            style = MaterialTheme.typography.titleMedium,
            color = Ash,
        )
        Spacer(Modifier.height(20.dp))
        Rule()
        Spacer(Modifier.height(20.dp))

        ProfilePhoto(profile.photoPath, fallbackInitial = profile.name)

        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text(profile.bio, style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
        if (profile.promptAnswer.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Eyebrow(Profile.PROMPT_QUESTION)
            Spacer(Modifier.height(8.dp))
            Text(profile.promptAnswer, style = MaterialTheme.typography.titleMedium, color = Ink)
        }

        if (isSelf) {
            Spacer(Modifier.height(36.dp))
            Rule()
            Spacer(Modifier.height(20.dp))
            Eyebrow("Your devices")
            Spacer(Modifier.height(8.dp))
            Text("Use Hubble on your computer too — your matches and chats follow.", style = MaterialTheme.typography.bodyMedium, color = Ash)
            Spacer(Modifier.height(12.dp))
            SignalButton("Sync to my computer", onClick = onSyncDevices)
            Spacer(Modifier.height(10.dp))
            SignalButton("Pair a new computer (scan QR)", onClick = onPairDesktop)
            Spacer(Modifier.height(20.dp))
            Eyebrow("Hubble ${net.tenbo.hubble.app.BuildConfig.VERSION_NAME}")
        }
    }
}

/**
 * Crossed-paths discovery — the heart of Hubble. One person at a time (deliberately
 * not an infinite swipe stack), each led by the coordinate stamp of where and when you
 * actually crossed paths. Pass is quiet; Like glows in signal blue.
 */
@Composable
fun DiscoveryScreen(
    encounters: List<net.tenbo.hubble.app.data.EncounterEntity>,
    nowMs: Long,
    onPass: (String) -> Unit,
    onLike: (String) -> Unit,
    onNearby: () -> Unit,
    onMatches: () -> Unit,
    onProfile: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 56.dp, bottom = 28.dp),
    ) {
        Wordmark()
        Spacer(Modifier.height(28.dp))

        val person = encounters.firstOrNull()
        if (person == null) {
            Eyebrow("Crossed paths ×0")
            Spacer(Modifier.height(16.dp))
            Text("No new crossings yet.", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text(
                "Hubble only shows people you've actually been near. Go where people " +
                    "are — open Nearby, and someone will turn up here.",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
            Spacer(Modifier.height(28.dp))
            SignalButton("Find people nearby", onClick = onNearby)
            return@Column
        }

        Eyebrow("${encounters.size} crossed your path")
        Spacer(Modifier.height(20.dp))
        CoordinateStamp(crossedStamp(person.place, person.lastSeenEpochMs, person.count, nowMs))
        Spacer(Modifier.height(16.dp))
        ProfilePhoto(person.photoPath, fallbackInitial = person.name)
        Spacer(Modifier.height(20.dp))
        Text("${person.name}, ${person.age}", style = MaterialTheme.typography.displayMedium, color = Ink)
        if (person.city.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(person.city, style = MaterialTheme.typography.titleMedium, color = Ash)
        }
        Spacer(Modifier.height(20.dp))
        Rule()
        if (person.bio.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(person.bio, style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
        if (person.promptAnswer.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Eyebrow(Profile.PROMPT_QUESTION)
            Spacer(Modifier.height(8.dp))
            Text(person.promptAnswer, style = MaterialTheme.typography.titleMedium, color = Ink)
        }

        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionButton("Pass", filled = false, modifier = Modifier.weight(1f)) { onPass(person.hubbleId) }
            ActionButton("Like", filled = true, modifier = Modifier.weight(1f)) { onLike(person.hubbleId) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "A like is private until they like you back.",
            style = MaterialTheme.typography.bodyMedium, color = Ash,
        )
    }
}

@Composable
private fun ActionButton(label: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (filled) Modifier.background(Signal) else Modifier.border(1.dp, Line, RoundedCornerShape(8.dp)))
            .clickableNoRipple(onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), style = MonoLabel.copy(fontSize = 13.sp), color = if (filled) net.tenbo.hubble.app.ui.theme.Paper else Ink)
    }
}

/** "place · 40m ago · ×2" — the encounter coordinate, formatted for the stamp. */
private fun crossedStamp(place: String, lastSeenMs: Long, count: Int, nowMs: Long): String {
    val delta = (nowMs - lastSeenMs).coerceAtLeast(0)
    val ago = when {
        delta < 60 * 60_000L -> "${delta / 60_000L}m ago"
        delta < 24 * 60 * 60_000L -> "${delta / (60 * 60_000L)}h ago"
        else -> "${delta / (24 * 60 * 60_000L)}d ago"
    }
    return "$place · $ago · ×$count"
}

/**
 * The coarse activity view — "where it's alive tonight". Not a street map: soft blobs
 * of where people are active, sized by how many, positioned roughly geographically.
 * The server only knows k-anonymous cells, so no individual can ever be located here.
 */
@Composable
fun MapScreen(cells: List<net.tenbo.hubble.app.net.HeatCell>, locationGranted: Boolean, onCheckIn: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 56.dp, bottom = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Wordmark(); GhostButton("Back", onClick = onBack)
        }
        Spacer(Modifier.height(28.dp))
        Eyebrow("Tonight")
        Spacer(Modifier.height(16.dp))
        Text("Where it's alive", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(16.dp))
        SignalButton("Check in here") { onCheckIn() }
        Spacer(Modifier.height(8.dp))
        Text("Adds you to your area's count — shown only when a few people are there.", style = MaterialTheme.typography.bodyMedium, color = Ash)
        Spacer(Modifier.height(20.dp))

        if (cells.isEmpty()) {
            Text("Quiet out there right now. Head somewhere and you'll light it up.", style = MaterialTheme.typography.bodyLarge, color = Ash)
            return@Column
        }

        OsmHeatmap(
            cells = cells,
            locationGranted = locationGranted,
            modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(12.dp)),
        )

        Spacer(Modifier.height(24.dp))
        Eyebrow("Areas alive")
        Spacer(Modifier.height(12.dp))
        cells.sortedByDescending { it.count }.forEach { c ->
            CoordinateStamp("${c.cell} · ${intensityLabel(c.count)}")
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun intensityLabel(count: Int): String = when {
    count >= 6 -> "buzzing"
    count >= 4 -> "lively"
    else -> "a few about"
}

/** Matches: people who liked you back. Tap one to open the E2E chat. */
@Composable
fun MatchesScreen(matches: List<MatchEntity>, onOpen: (String) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 56.dp, bottom = 28.dp),
    ) {
        Wordmark()
        Spacer(Modifier.height(28.dp))
        Eyebrow("Matches")
        Spacer(Modifier.height(20.dp))

        if (matches.isEmpty()) {
            Text("No matches yet.", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text(
                "When someone you liked likes you back, they land here — and you can finally talk.",
                style = MaterialTheme.typography.bodyLarge, color = Ash,
            )
            return@Column
        }

        matches.forEach { m ->
            Row(
                Modifier.fillMaxWidth().clickableNoRipple { onOpen(m.hubbleId) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(m.photoPath, m.name)
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

/** The E2E chat with a match. Mine glow signal; theirs are quiet paper. */
@Composable
fun ChatScreen(
    match: MatchEntity?,
    messages: List<MessageEntity>,
    typing: Boolean,
    onSend: (String) -> Unit,
    onSendVoice: (java.io.File, Long) -> Unit,
    onTyping: () -> Unit,
    onUnmatch: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Voice recording (tap mic to start, tap again to stop & send).
    val recorder = remember { net.tenbo.hubble.app.audio.VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    val micPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted && recorder.start()) recording = true }
    fun toggleRecord() {
        if (recording) {
            recording = false
            recorder.stop()?.let { (file, dur) -> onSendVoice(file, dur) }
        } else {
            val granted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) { if (recorder.start()) recording = true }
            else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Which voice clip is playing right now (shared so only one plays at a time).
    var playingPath by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { net.tenbo.hubble.app.audio.VoicePlayer.stop() } }
    Column(Modifier.fillMaxSize().padding(top = 52.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("Back", onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text(match?.name ?: "", style = MaterialTheme.typography.headlineSmall, color = Ink)
            }
            var menu by remember { mutableStateOf(false) }
            Box {
                GhostButton("⋯") { menu = true }
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Unmatch") }, onClick = { menu = false; onUnmatch() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Block") }, onClick = { menu = false; onBlock() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Report") }, onClick = { menu = false; onReport() })
                }
            }
        }
        Rule()

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            if (match != null) {
                CoordinateStamp("matched · ${match.place}")
                Spacer(Modifier.height(16.dp))
            }
            messages.forEach { msg ->
                Bubble(
                    msg = msg,
                    playing = msg.audioPath != null && msg.audioPath == playingPath,
                    onTogglePlay = { path ->
                        net.tenbo.hubble.app.audio.VoicePlayer.toggle(path) { now -> playingPath = now }
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            if (typing) {
                Eyebrow("${match?.name ?: "They"} is typing…")
            }
        }

        Rule()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it; onTyping() },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    cursorBrush = SolidColor(Signal),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Message", style = MaterialTheme.typography.bodyLarge, color = Ash)
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            if (draft.isBlank()) {
                // Mic: tap to record, tap again to stop & send. Glows signal-blue while recording.
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (recording) Signal else PaperSunk)
                        .clickableNoRipple { keyboard?.hide(); focus.clearFocus(); toggleRecord() }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (recording) "● STOP" else "🎤",
                        style = MonoLabel.copy(fontSize = 12.sp),
                        color = if (recording) Paper else Ink,
                    )
                }
            } else {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Signal)
                        .clickableNoRipple { onSend(draft); draft = ""; keyboard?.hide(); focus.clearFocus() }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text("SEND", style = MonoLabel.copy(fontSize = 12.sp), color = Paper)
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: MessageEntity, playing: Boolean = false, onTogglePlay: (String) -> Unit = {}) {
    val mine = msg.fromMe
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier.fillMaxWidth(0.82f).wrapContentWidth(if (mine) Alignment.End else Alignment.Start)
                .clip(RoundedCornerShape(14.dp))
                .background(if (mine) Signal else PaperSunk)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val path = msg.audioPath
            if (msg.kind == "voice" && path != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (playing) "⏸" else "▶",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (mine) Paper else Signal,
                        modifier = Modifier.clickableNoRipple { onTogglePlay(path) },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Voice · ${formatDuration(msg.durationMs)}",
                        style = MonoLabel.copy(fontSize = 12.sp),
                        color = if (mine) Paper else Ink,
                    )
                }
            } else {
                Text(msg.text, style = MaterialTheme.typography.bodyLarge, color = if (mine) Paper else Ink)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

/** The "it's a match" moment — full-bleed, editorial, grounded in where you crossed. */
@Composable
fun MatchCelebration(match: MatchEntity, onMessage: () -> Unit, onKeepLooking: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Paper).padding(horizontal = 28.dp).padding(top = 80.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow("You both said yes")
        Spacer(Modifier.height(24.dp))
        Avatar(match.photoPath, match.name, sizeDp = 120)
        Spacer(Modifier.height(28.dp))
        Text("You and ${match.name}", style = MaterialTheme.typography.displayMedium, color = Ink)
        Spacer(Modifier.height(12.dp))
        CoordinateStamp("crossed paths at ${match.place}")
        Spacer(Modifier.weight(1f))
        SignalButton("Send a message", onClick = onMessage)
        Spacer(Modifier.height(4.dp))
        GhostButton("Keep looking", modifier = Modifier.fillMaxWidth(), onClick = onKeepLooking)
    }
}

@Composable
private fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            cursorBrush = SolidColor(Signal),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.padding(bottom = 8.dp)) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Ash)
                    inner()
                }
            },
        )
        Rule()
    }
}

private fun copyToInternal(context: Context, uri: Uri): String? = runCatching {
    val file = File(context.filesDir, "profile_photo.jpg")
    context.contentResolver.openInputStream(uri)!!.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.absolutePath
}.getOrNull()
