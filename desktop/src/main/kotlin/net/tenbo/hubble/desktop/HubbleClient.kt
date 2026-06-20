package net.tenbo.hubble.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.friend.FriendRecord
import net.tenbo.hubble.core.identity.Identity
import net.tenbo.hubble.core.identity.IdentityFactory
import net.tenbo.hubble.core.identity.Mnemonic
import net.tenbo.hubble.core.message.ChatMessage
import net.tenbo.hubble.core.message.EnvelopeCodec
import net.tenbo.hubble.core.message.Incoming
import net.tenbo.hubble.core.message.MatchSnapshot
import net.tenbo.hubble.core.message.MatchSnapshotCodec
import net.tenbo.hubble.core.message.Messaging
import net.tenbo.hubble.core.message.SelfSync
import java.util.UUID

/** Sentinel sent as a chat message for a nudge; rendered specially + shakes the window. */
const val NUDGE_MARKER = "📳 sent a nudge!"

data class ChatLine(val fromMe: Boolean, val text: String, val at: Long)

/** A classic MSN corner toast: who, and a preview. */
data class Toast(val hubbleId: String, val name: String, val text: String)

/** A chat message destined for [recipientMailbox], readable only by the matched peer. */

/**
 * Desktop app state + logic. A network-participating companion: restores the identity
 * from the recovery phrase, pulls matches the phone shared via self-sync, and chats
 * E2E over the relay. Holds Compose state directly (no Android ViewModel here).
 */
class HubbleClient(private val scope: CoroutineScope) {
    private val crypto = BouncyCastleCrypto()
    private val factory = IdentityFactory(crypto)
    private val api = DesktopApi()

    var identity by mutableStateOf<Identity?>(null); private set
    var matches by mutableStateOf<List<MatchSnapshot>>(emptyList()); private set
    var current by mutableStateOf<MatchSnapshot?>(null); private set
    var messages by mutableStateOf<List<ChatLine>>(emptyList()); private set
    var status by mutableStateOf("Not connected"); private set
    var restoreError by mutableStateOf<String?>(null); private set
    var showMap by mutableStateOf(false); private set
    var heat by mutableStateOf<List<HeatCell>>(emptyList()); private set
    var toast by mutableStateOf<Toast?>(null); private set
    var soundOn by mutableStateOf(Store.loadSoundEnabled()); private set
    var typingFrom by mutableStateOf<String?>(null); private set
    private var typingExpiry = 0L
    private var lastTypingSent = 0L

    private val threads = mutableMapOf<String, MutableList<ChatLine>>()

    init {
        Store.loadPhrase()?.let { restore(it) }
        scope.launch { while (true) { delay(2_500); poll() } }
        // Clear the "is typing…" indicator once it goes stale.
        scope.launch { while (true) { delay(1_000); if (typingFrom != null && System.currentTimeMillis() > typingExpiry) typingFrom = null } }
    }

    fun toggleSound() { soundOn = !soundOn; Store.saveSoundEnabled(soundOn) }

    var presence by mutableStateOf(runCatching { Presence.valueOf(Store.loadPresence()) }.getOrDefault(Presence.ONLINE)); private set
    fun changePresence(p: Presence) { presence = p; Store.savePresence(p.name) }

    /** Bumped whenever a nudge is sent/received, so the chat window can shake. */
    var nudge by mutableStateOf(0); private set
    fun sendNudge() = send(NUDGE_MARKER)

    /** Restore identity from a recovery phrase; persist it and pull matches. */
    fun restore(phrase: String) {
        val p = phrase.trim().lowercase()
        if (!Mnemonic.isValid(p)) { restoreError = "That's not a valid 12-word recovery phrase."; return }
        restoreError = null
        identity = factory.fromPhrase(p)
        Store.savePhrase(p)
        status = "Connected as ${identity!!.hubbleId}"
        if (soundOn) Sound.signIn()
        sync()
    }

    fun signOut() { Store.clear(); identity = null; matches = emptyList(); current = null; status = "Not connected" }

    /** Pull the matches the phone shared via the encrypted self-mailbox. */
    fun sync() {
        val id = identity ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val self = SelfSync(crypto, id)
                    val blobs = api.collect(self.mailboxId)
                    val pulled = blobs.flatMap { MatchSnapshotCodec.decodeList(self.open(it)) }
                    if (pulled.isNotEmpty()) matches = (pulled + matches).distinctBy { it.hubbleId }
                    status = if (api.health()) "Online · ${matches.size} matches" else "Server unreachable"
                }.onFailure { status = "Sync failed: ${it.message}" }
            }
        }
    }

    fun openMap() { showMap = true; loadHeatmap() }
    fun closeMap() { showMap = false }

    /** Fetch the coarse activity heatmap (seeding a few demo cells so it has life). */
    fun loadHeatmap() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                if (api.heatmap().isEmpty()) {
                    mapOf("u173zw" to 6, "u173zt" to 4, "u173zk" to 3, "u15g8x" to 5)
                        .forEach { (cell, n) -> repeat(n) { runCatching { api.postBeacon(cell) } } }
                }
                heat = api.heatmap()
            }.onFailure { status = "Heatmap failed: ${it.message}" }
        }
    }

    /** Post a single presence beacon for a coarse area ("check in"). One person = one
     *  beacon; a lone cell stays hidden by k-anonymity until others are there too. */
    fun checkIn(geohash: String) {
        scope.launch(Dispatchers.IO) {
            runCatching { api.postBeacon(geohash); heat = api.heatmap() }
                .onFailure { status = "Check-in failed: ${it.message}" }
        }
    }

    fun openChat(m: MatchSnapshot) {
        current = m
        messages = threads.getOrPut(m.hubbleId) { mutableListOf() }.toList()
    }

    fun closeChat() { current = null }

    fun send(text: String) {
        val id = identity ?: return
        val m = current ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        val now = System.currentTimeMillis()
        appendLine(m.hubbleId, ChatLine(true, body, now))
        scope.launch(Dispatchers.IO) {
            runCatching {
                val chat = ChatMessage(UUID.randomUUID().toString(), id.hubbleId, body, now)
                val env = Messaging(crypto, id).sealChat(chat, m.toFriendRecord())
                api.deposit(env.recipientMailbox, EnvelopeCodec.encode(env))
            }.onFailure { status = "Send failed: ${it.message}" }
        }
    }

    /** Tell the current match we're typing (throttled so we don't spam the relay). */
    fun notifyTyping() {
        val id = identity ?: return
        val m = current ?: return
        val now = System.currentTimeMillis()
        if (now - lastTypingSent < 2_500) return
        lastTypingSent = now
        scope.launch(Dispatchers.IO) {
            runCatching {
                val env = Messaging(crypto, id).sealTyping(m.toFriendRecord())
                api.deposit(env.recipientMailbox, EnvelopeCodec.encode(env))
            }
        }
    }

    private suspend fun poll() {
        val id = identity ?: return
        withContext(Dispatchers.IO) {
            val messaging = Messaging(crypto, id)
            val byTag = matches.associateBy { messaging.expectedSenderTag(it.toFriendRecord()) }
            runCatching {
                for (bytes in api.collect(messaging.myMailboxId())) {
                    runCatching {
                        val env = EnvelopeCodec.decode(bytes)
                        val m = byTag[env.senderTag] ?: return@runCatching
                        when (val incoming = messaging.open(env, m.toFriendRecord())) {
                            is Incoming.Chat -> appendLine(m.hubbleId, ChatLine(false, incoming.message.text, incoming.message.sentAtMs))
                            is Incoming.Typing -> { typingFrom = m.hubbleId; typingExpiry = System.currentTimeMillis() + 6_000 }
                            is Incoming.Post -> {} // posts aren't shown in the desktop companion
                        }
                    }
                }
            }
        }
    }

    private fun appendLine(hubbleId: String, line: ChatLine) {
        val list = threads.getOrPut(hubbleId) { mutableListOf() }
        list.add(line)
        if (current?.hubbleId == hubbleId) messages = list.toList()
        if (line.text == NUDGE_MARKER && current?.hubbleId == hubbleId) nudge++
        if (!line.fromMe) {
            if (soundOn) Sound.message()
            // Pop the classic corner toast for a message you're not already reading.
            if (current?.hubbleId != hubbleId) {
                val name = matches.firstOrNull { it.hubbleId == hubbleId }?.name ?: "Someone"
                toast = Toast(hubbleId, name, line.text)
            }
        }
    }

    fun dismissToast() { toast = null }

    fun openFromToast() {
        val t = toast ?: return
        matches.firstOrNull { it.hubbleId == t.hubbleId }?.let { openChat(it) }
        toast = null
    }

    private fun MatchSnapshot.toFriendRecord() =
        FriendRecord(hubbleId, name, signingPublicKey, agreementPublicKey, rootKey, matchedAtMs)
}
