package net.tenbo.hubble.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tenbo.hubble.app.data.BlockDao
import net.tenbo.hubble.app.data.BlockEntity
import net.tenbo.hubble.app.data.EncounterDao
import net.tenbo.hubble.app.data.EncounterEntity
import net.tenbo.hubble.app.data.MatchDao
import net.tenbo.hubble.app.data.MatchEntity
import net.tenbo.hubble.app.data.MessageDao
import net.tenbo.hubble.app.data.MessageEntity
import net.tenbo.hubble.app.data.FriendDao
import net.tenbo.hubble.app.data.FriendEntity
import net.tenbo.hubble.app.data.KeystoreVault
import net.tenbo.hubble.app.data.PostDao
import net.tenbo.hubble.app.data.Profile
import net.tenbo.hubble.app.data.PostEntity
import net.tenbo.hubble.app.friendship.FriendshipOrchestrator
import net.tenbo.hubble.app.net.Geohash
import net.tenbo.hubble.app.net.HeatCell
import net.tenbo.hubble.app.net.HubbleApi
import net.tenbo.hubble.app.net.Locator
import net.tenbo.hubble.app.p2p.WebRtcManager
import net.tenbo.hubble.app.p2p.WebRtcTransport
import net.tenbo.hubble.app.proximity.BleEnvironment
import net.tenbo.hubble.app.proximity.NearbyHubble
import net.tenbo.hubble.app.proximity.ProximityCoordinator
import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.handshake.HandshakeState
import net.tenbo.hubble.core.identity.Mnemonic
import net.tenbo.hubble.core.message.ChatMessage
import net.tenbo.hubble.core.message.EnvelopeCodec
import net.tenbo.hubble.core.message.EphemeralPost
import net.tenbo.hubble.core.message.Incoming
import net.tenbo.hubble.core.message.MatchSnapshot
import net.tenbo.hubble.core.message.MatchSnapshotCodec
import net.tenbo.hubble.core.message.Messaging
import net.tenbo.hubble.core.message.SelfSync
import net.tenbo.hubble.core.message.VoiceMessage
import net.tenbo.hubble.app.audio.VOICE_MIME
import java.io.File
import net.tenbo.hubble.core.transport.Transport
import kotlinx.coroutines.Dispatchers
import android.util.Log
import java.security.SecureRandom
import java.util.UUID

/** Which top-level screen the app is showing. */
enum class Screen { ONBOARDING, DISCOVERY, MATCHES, CHAT, NEARBY, MAP, SAS_CONFIRM, FRIENDS, FEED, COMPOSE, PROFILE, PROFILE_EDIT }

/**
 * App-level UI state: onboarding vs. ready, the pending recovery phrase, the live
 * handshake state, nearby devices, and the friends list. Owns the proximity flow.
 */
class HubbleViewModel(
    private val appContext: Context,
    private val vault: KeystoreVault,
    private val friendDao: FriendDao,
    private val postDao: PostDao,
    private val encounterDao: EncounterDao,
    private val matchDao: MatchDao,
    private val messageDao: MessageDao,
    private val blockDao: BlockDao,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    @Volatile private var blockedIds: Set<String> = emptySet()

    private val _screen = MutableStateFlow(if (vault.hasIdentity()) Screen.DISCOVERY else Screen.ONBOARDING)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendEntity>>(emptyList())
    val friends: StateFlow<List<FriendEntity>> = _friends.asStateFlow()

    private val _posts = MutableStateFlow<List<PostEntity>>(emptyList())
    val posts: StateFlow<List<PostEntity>> = _posts.asStateFlow()

    /** Default lifetime of an ephemeral post: 24 hours. */
    private val postTtlMs = 24 * 60 * 60 * 1000L

    private val _nearby = MutableStateFlow<List<NearbyHubble>>(emptyList())
    val nearby: StateFlow<List<NearbyHubble>> = _nearby.asStateFlow()

    private val _encounters = MutableStateFlow<List<EncounterEntity>>(emptyList())
    val encounters: StateFlow<List<EncounterEntity>> = _encounters.asStateFlow()

    private val _matches = MutableStateFlow<List<MatchEntity>>(emptyList())
    val matches: StateFlow<List<MatchEntity>> = _matches.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    /** Set when a like completes a match, so the UI can show the celebration. */
    private val _justMatched = MutableStateFlow<MatchEntity?>(null)
    val justMatched: StateFlow<MatchEntity?> = _justMatched.asStateFlow()

    private val _currentMatch = MutableStateFlow<MatchEntity?>(null)
    val currentMatch: StateFlow<MatchEntity?> = _currentMatch.asStateFlow()
    private var currentMatchId: String? = null

    /** hubbleId of a match currently typing to us (cleared when stale). */
    private val _typingFrom = MutableStateFlow<String?>(null)
    val typingFrom: StateFlow<String?> = _typingFrom.asStateFlow()
    private var typingExpiry = 0L
    private var lastTypingSent = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) { blockedIds = blockDao.allIds().toSet() }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (_typingFrom.value != null && clockMs() > typingExpiry) _typingFrom.value = null
            }
        }
    }

    /** Remove a match (and its messages); keep the door open to match again later. */
    fun unmatch(hubbleId: String) {
        viewModelScope.launch {
            matchDao.delete(hubbleId); messageDao.deleteForMatch(hubbleId)
            loadMatches(); _screen.value = Screen.MATCHES
        }
    }

    /** Block (optionally report) someone: remove match+messages+encounter and bar them. */
    fun block(hubbleId: String, report: Boolean = false) {
        viewModelScope.launch {
            blockDao.block(BlockEntity(hubbleId, report, clockMs()))
            matchDao.delete(hubbleId); messageDao.deleteForMatch(hubbleId); encounterDao.delete(hubbleId)
            blockedIds = blockDao.allIds().toSet()
            loadMatches(); loadEncounters(); _screen.value = Screen.MATCHES
        }
    }

    fun report(hubbleId: String) = block(hubbleId, report = true)

    private val _heatmap = MutableStateFlow<List<HeatCell>>(emptyList())
    val heatmap: StateFlow<List<HeatCell>> = _heatmap.asStateFlow()

    /** A freshly generated phrase awaiting the user's "I backed it up / skip". */
    var pendingPhrase: String = generatePhrase()
        private set

    var orchestrator: FriendshipOrchestrator? = null
        private set

    private var coordinator: ProximityCoordinator? = null
    private var proximityStarted = false

    private val crypto = BouncyCastleCrypto()
    private val api = HubbleApi()
    private val tag = "HubbleSync"

    private var webrtc: WebRtcManager? = null
    private var p2pStarted = false

    val handshakeState: StateFlow<HandshakeState>? get() = orchestrator?.state

    private fun generatePhrase(): String =
        Mnemonic.generate(ByteArray(16).also { SecureRandom().nextBytes(it) })

    /** Complete onboarding: persist phrase + profile and build the orchestrator. */
    fun completeOnboarding(profile: Profile) {
        vault.savePhrase(pendingPhrase)
        vault.saveProfile(profile)
        buildOrchestrator()
        _screen.value = Screen.DISCOVERY
    }

    fun currentProfile(): Profile = vault.loadProfile()

    fun saveProfile(profile: Profile) {
        vault.saveProfile(profile)
        _screen.value = Screen.PROFILE
    }

    /** Load the active (non-expired) feed, purging anything that has lapsed. */
    fun loadPosts() {
        viewModelScope.launch {
            val now = clockMs()
            postDao.purgeExpired(now)
            _posts.value = postDao.active(now)
        }
    }

    /** Publish a new ephemeral post: store locally and deliver to every friend's mailbox. */
    fun publishPost(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            val now = clockMs()
            val identity = vault.loadIdentity()
            val postId = UUID.randomUUID().toString()
            postDao.upsert(
                PostEntity(
                    id = postId,
                    authorHubbleId = identity?.hubbleId ?: "me",
                    authorName = vault.displayName().ifBlank { "Me" },
                    text = body,
                    createdAtEpochMs = now,
                    expiresAtEpochMs = now + postTtlMs,
                    mine = true,
                ),
            )
            _screen.value = Screen.FEED
            loadPosts()

            // Seal the post for each friend and deposit it to their relay mailbox.
            if (identity != null) {
                val post = EphemeralPost(postId, identity.hubbleId, body, now, postTtlMs)
                val messaging = Messaging(crypto, identity)
                launch(Dispatchers.IO) {
                    runCatching {
                        friendDao.all().forEach { friend ->
                            val envelope = messaging.sealPost(post, friend.toRecord())
                            api.deposit(envelope.recipientMailbox, EnvelopeCodec.encode(envelope))
                        }
                    }.onFailure { Log.w(tag, "deposit failed: ${it.message}") }
                }
            }
        }
    }

    /** Pull our relay mailbox, decrypt envelopes from friends, and add their posts. */
    fun sync() {
        viewModelScope.launch(Dispatchers.IO) {
            val identity = vault.loadIdentity() ?: return@launch
            val messaging = Messaging(crypto, identity)
            runCatching {
                val mailboxId = messaging.myMailboxId()
                val envelopes = api.collect(mailboxId)
                Log.i(tag, "synced mailbox $mailboxId: ${envelopes.size} envelope(s)")
                var added = false
                for (bytes in envelopes) if (ingestEnvelope(bytes)) added = true
                if (added) loadPosts()
            }.onFailure { Log.w(tag, "sync failed: ${it.message}") }
        }
    }

    /**
     * Decode one envelope, match it to a friend by sender tag, decrypt+verify, and
     * store the post. Returns true if a post was added. One malformed/foreign envelope
     * never throws out of here. Shared by relay sync and the WebRTC data channel.
     */
    private suspend fun ingestEnvelope(bytes: ByteArray): Boolean {
        val identity = vault.loadIdentity() ?: return false
        val messaging = Messaging(crypto, identity)
        val matchByTag = matchDao.all().associateBy { messaging.expectedSenderTag(it.toFriendRecord()) }
        val friendByTag = friendDao.all().associateBy { messaging.expectedSenderTag(it.toRecord()) }
        return runCatching {
            val envelope = EnvelopeCodec.decode(bytes)
            val match = matchByTag[envelope.senderTag]
            val friend = friendByTag[envelope.senderTag]
            val record = match?.toFriendRecord() ?: friend?.toRecord() ?: return@runCatching false
            if (record.hubbleId in blockedIds) return@runCatching false // drop anything from blocked people
            when (val incoming = messaging.open(envelope, record)) {
                is Incoming.Chat -> {
                    val matchId = match?.hubbleId ?: return@runCatching false
                    val m = incoming.message
                    messageDao.insert(MessageEntity(m.id, matchId, fromMe = false, text = m.text, sentAtEpochMs = m.sentAtMs))
                    matchDao.touch(matchId, m.sentAtMs)
                    if (currentMatchId == matchId) _messages.value = messageDao.forMatch(matchId)
                    true
                }
                is Incoming.Post -> {
                    val p = incoming.post
                    postDao.upsert(
                        PostEntity(
                            id = p.id,
                            authorHubbleId = p.authorHubbleId,
                            authorName = match?.name ?: friend?.displayName ?: "Someone",
                            text = p.text,
                            createdAtEpochMs = p.createdAtMs,
                            expiresAtEpochMs = p.createdAtMs + p.ttlMs,
                            mine = false,
                        ),
                    )
                    true
                }
                is Incoming.Voice -> {
                    val matchId = match?.hubbleId ?: return@runCatching false
                    val v = incoming.message
                    val stored = persistVoice(v.id, v.audio)
                    messageDao.insert(
                        MessageEntity(
                            v.id, matchId, fromMe = false, text = "Voice message", sentAtEpochMs = v.sentAtMs,
                            kind = "voice", audioPath = stored.absolutePath, durationMs = v.durationMs,
                        ),
                    )
                    matchDao.touch(matchId, v.sentAtMs)
                    if (currentMatchId == matchId) _messages.value = messageDao.forMatch(matchId)
                    true
                }
                is Incoming.Typing -> {
                    if (match != null) { _typingFrom.value = match.hubbleId; typingExpiry = clockMs() + 6_000 }
                    false
                }
            }
        }.getOrDefault(false)
    }

    /** Bring up WebRTC: init the factory and connect signaling under our mailbox id. */
    fun initP2p() {
        if (p2pStarted) return
        val identity = vault.loadIdentity() ?: return
        p2pStarted = true
        val myMailbox = Messaging(crypto, identity).myMailboxId()
        webrtc = WebRtcManager(appContext, viewModelScope, myMailbox) { _, transport ->
            consumeTransport(transport)
        }.also { it.start() }
    }

    /** Drain a direct (WebRTC) transport, feeding posts into the feed as they arrive. */
    private fun consumeTransport(transport: WebRtcTransport) {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val bytes = runCatching { transport.receive() }.getOrNull() ?: break
                if (ingestEnvelope(bytes)) loadPosts()
            }
        }
    }

    fun ensureOrchestrator() {
        if (orchestrator == null && vault.hasIdentity()) buildOrchestrator()
    }

    private fun buildOrchestrator() {
        val identity = vault.loadIdentity() ?: return
        orchestrator = FriendshipOrchestrator(identity, vault.displayName(), friendDao)
    }

    /** Begin advertising + scanning (call when Nearby is shown and permissions granted). */
    fun startProximity() {
        if (proximityStarted) return
        val env = BleEnvironment(appContext)
        if (!env.isBluetoothReady) return
        val coord = ProximityCoordinator(appContext, viewModelScope, env)
        coordinator = coord
        proximityStarted = true

        coord.start()

        // Mirror discovered devices to the UI.
        viewModelScope.launch { coord.nearby.collect { _nearby.value = it } }

        // Responder: a peer connected to us and started the handshake.
        viewModelScope.launch {
            for (transport in coord.incomingTransports) {
                runHandshake(transport)
            }
        }
    }

    /** Initiator: the user tapped a nearby hubble to add them. */
    fun addNearby(hubble: NearbyHubble) {
        val coord = coordinator ?: return
        viewModelScope.launch {
            runCatching { coord.connect(hubble) }.onSuccess { runHandshake(it) }
        }
    }

    private suspend fun runHandshake(transport: Transport) {
        ensureOrchestrator()
        _screen.value = Screen.SAS_CONFIRM
        orchestrator?.begin(transport)
    }

    /** Load undecided crossings, seeding a few samples the first time so the screen isn't bare. */
    fun loadEncounters() {
        viewModelScope.launch {
            if (encounterDao.total() == 0) seedSampleEncounters()
            _encounters.value = encounterDao.pending().filterNot { it.hubbleId in blockedIds }
        }
    }

    /** Like a crossing. If they already liked you, it becomes a match (with a celebration). */
    fun likeEncounter(hubbleId: String) {
        viewModelScope.launch {
            val enc = _encounters.value.firstOrNull { it.hubbleId == hubbleId }
            encounterDao.decide(hubbleId, liked = true)
            if (enc != null && enc.likesYouBack) {
                val now = clockMs()
                val match = MatchEntity(
                    hubbleId = enc.hubbleId, name = enc.name, age = enc.age, city = enc.city,
                    photoPath = enc.photoPath, place = enc.place,
                    signingPublicKey = enc.signingPublicKey,
                    agreementPublicKey = enc.agreementPublicKey,
                    rootKey = enc.rootKey,
                    matchedAtEpochMs = now, lastActivityEpochMs = now,
                )
                matchDao.upsert(match)
                messageDao.insert(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        matchHubbleId = enc.hubbleId,
                        fromMe = false,
                        text = "Funny — Hubble says we keep ending up in the same places.",
                        sentAtEpochMs = now,
                    ),
                )
                _justMatched.value = match
            }
            _encounters.value = encounterDao.pending()
        }
    }

    fun passEncounter(hubbleId: String) {
        viewModelScope.launch {
            encounterDao.decide(hubbleId, liked = false)
            _encounters.value = encounterDao.pending()
        }
    }

    fun dismissMatch() { _justMatched.value = null }

    /** Transient banner for the pair-desktop flow ("Paired", "QR not recognized", etc.). */
    private val _pairingStatus = MutableStateFlow<String?>(null)
    val pairingStatus: StateFlow<String?> = _pairingStatus.asStateFlow()

    fun dismissPairingStatus() { _pairingStatus.value = null }

    /**
     * Pair a freshly-installed desktop client by scanning the QR it shows. We read the
     * desktop's ephemeral pubkey from the QR, derive a one-shot symmetric key via ECDH,
     * encrypt our recovery phrase under it, and deposit the envelope in the desktop-derived
     * mailbox. The desktop polls that mailbox, decrypts, and restores the same identity.
     * Then it self-syncs and pulls our matches.
     */
    fun pairDesktop(qrPayload: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val phrase = vault.loadPhrase()
            if (phrase == null) {
                _pairingStatus.value = "No phrase yet — finish onboarding first."
                return@launch
            }
            val sealed = runCatching {
                net.tenbo.hubble.core.pair.Pairing(crypto).phoneSeal(qrPayload, phrase)
            }.getOrElse {
                _pairingStatus.value = "That QR isn't a Hubble pairing code."
                return@launch
            }
            runCatching { api.deposit(sealed.mailboxId, sealed.envelope) }
                .onSuccess { _pairingStatus.value = "Paired — your computer is now signed in." }
                .onFailure {
                    Log.w(tag, "pair deposit failed: ${it.message}")
                    _pairingStatus.value = "Couldn't reach the relay — check your connection."
                }
        }
    }

    /** Publish your matches to the encrypted self-mailbox so your other devices (desktop) can pull them. */
    fun syncToMyDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            val identity = vault.loadIdentity() ?: return@launch
            val snaps = matchDao.all().map {
                MatchSnapshot(it.hubbleId, it.name, it.age, it.city, it.place, it.signingPublicKey, it.agreementPublicKey, it.rootKey, it.matchedAtEpochMs)
            }
            val self = SelfSync(crypto, identity)
            runCatching { api.deposit(self.mailboxId, self.seal(MatchSnapshotCodec.encodeList(snaps))) }
                .onFailure { Log.w(tag, "device sync failed: ${it.message}") }
        }
    }

    /** Fetch the coarse activity heatmap (seeding a few demo cells so the view has life). */
    fun loadHeatmap() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (api.heatmap().isEmpty()) seedDemoBeacons()
                _heatmap.value = api.heatmap()
            }.onFailure { Log.w(tag, "heatmap failed: ${it.message}") }
        }
    }

    /** Post a beacon for your real (coarse) location, then refresh the heatmap. */
    fun postLocationBeacon() {
        viewModelScope.launch(Dispatchers.IO) {
            val ll = Locator.coarse(appContext) ?: run { Log.w(tag, "no location fix"); return@launch }
            val cell = Geohash.encode(ll.first, ll.second, 6)
            runCatching { api.postBeacon(cell); _heatmap.value = api.heatmap() }
                .onFailure { Log.w(tag, "beacon failed: ${it.message}") }
        }
    }

    // Demo activity around NL so the map isn't empty. In production beacons are posted
    // by real users from their own coarse geohash (k-anonymity hides individuals).
    private fun seedDemoBeacons() {
        val cells = mapOf(
            "u173zw" to 6, // central Amsterdam
            "u173zt" to 4, // east
            "u173zk" to 3, // south
            "u15g8x" to 5, // Haarlem
        )
        cells.forEach { (cell, n) -> repeat(n) { runCatching { api.postBeacon(cell) } } }
    }

    fun loadMatches() {
        viewModelScope.launch { _matches.value = matchDao.all() }
    }

    fun openChat(hubbleId: String) {
        viewModelScope.launch {
            currentMatchId = hubbleId
            _currentMatch.value = matchDao.byId(hubbleId)
            _messages.value = messageDao.forMatch(hubbleId)
            _screen.value = Screen.CHAT
        }
    }

    /** Tell the open match we're typing (throttled), so their app can show "is typing…". */
    fun notifyTyping() {
        val match = _currentMatch.value ?: return
        val now = clockMs()
        if (now - lastTypingSent < 2_500) return
        lastTypingSent = now
        viewModelScope.launch(Dispatchers.IO) {
            val identity = vault.loadIdentity() ?: return@launch
            runCatching {
                val env = Messaging(crypto, identity).sealTyping(match.toFriendRecord())
                api.deposit(env.recipientMailbox, EnvelopeCodec.encode(env))
            }
        }
    }

    fun sendMessage(text: String) {
        val id = currentMatchId ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            val now = clockMs()
            val msgId = UUID.randomUUID().toString()
            messageDao.insert(MessageEntity(msgId, id, true, body, now))
            matchDao.touch(id, now)
            _messages.value = messageDao.forMatch(id)

            // Deliver E2E over the relay: seal the chat with the match's root key.
            val identity = vault.loadIdentity() ?: return@launch
            val match = matchDao.byId(id) ?: return@launch
            val chat = ChatMessage(msgId, identity.hubbleId, body, now)
            val envelope = Messaging(crypto, identity).sealChat(chat, match.toFriendRecord())
            launch(Dispatchers.IO) {
                runCatching { api.deposit(envelope.recipientMailbox, EnvelopeCodec.encode(envelope)) }
                    .onFailure { Log.w(tag, "chat deposit failed: ${it.message}") }
            }
        }
    }

    /** Send a recorded voice clip to the open match: store locally + seal over the relay. */
    fun sendVoice(file: File, durationMs: Long) {
        val id = currentMatchId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val now = clockMs()
            val msgId = UUID.randomUUID().toString()
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@launch
            file.delete() // the recorder wrote a temp file; we keep our own copy
            val stored = persistVoice(msgId, bytes)
            messageDao.insert(
                MessageEntity(
                    msgId, id, fromMe = true, text = "Voice message", sentAtEpochMs = now,
                    kind = "voice", audioPath = stored.absolutePath, durationMs = durationMs,
                ),
            )
            matchDao.touch(id, now)
            _messages.value = messageDao.forMatch(id)

            val identity = vault.loadIdentity() ?: return@launch
            val match = matchDao.byId(id) ?: return@launch
            val voice = VoiceMessage(msgId, identity.hubbleId, VOICE_MIME, durationMs, now, bytes)
            val envelope = Messaging(crypto, identity).sealVoice(voice, match.toFriendRecord())
            runCatching { api.deposit(envelope.recipientMailbox, EnvelopeCodec.encode(envelope)) }
                .onFailure { Log.w(tag, "voice deposit failed: ${it.message}") }
        }
    }

    /** Persist voice clip bytes to app-private storage and return the file. */
    private fun persistVoice(id: String, bytes: ByteArray): File {
        val dir = File(appContext.filesDir, "voice").apply { mkdirs() }
        return File(dir, "$id.3gp").apply { writeBytes(bytes) }
    }

    // Sample crossings so the discovery screen is reviewable before real BLE encounters
    // exist. In production these rows are written when you cross paths with someone
    // running Hubble and exchange profiles over the proximity handshake.
    private suspend fun seedSampleEncounters() {
        val now = clockMs()
        val min = 60_000L
        // Each sample carries a (demo) keypair, as a real encounter would from the handshake.
        fun keys(seed: Byte): Triple<ByteArray, ByteArray, ByteArray> {
            val s = ByteArray(32) { seed }
            return Triple(
                crypto.generateEd25519(s).publicKey,
                crypto.generateX25519(s).publicKey,
                crypto.hkdf(s, byteArrayOf(), "demo-root".toByteArray(), 32),
            )
        }
        val (ms, ma, mr) = keys(10); val (ts, ta, tr) = keys(11); val (ns, na, nr) = keys(12)
        val samples = listOf(
            EncounterEntity("demo-mara", "Mara", 27, "Amsterdam", "Cartographer. I collect other people's shortcuts.", "the corner table at Café Léon", null, "Café Léon", now - 40 * min, 2, false, false, true, ms, ma, mr),
            EncounterEntity("demo-tycho", "Tycho", 31, "Amsterdam", "Builds synths, terrible at small talk, great at long walks.", "the canal bench on Brouwersgracht", null, "Brouwersgracht", now - 3 * 60 * min, 1, false, false, false, ts, ta, tr),
            EncounterEntity("demo-noor", "Noor", 29, "Haarlem", "Bookshop regular. Will judge you (kindly) by your margins.", "the poetry shelf at De Vries", null, "Boekhandel De Vries", now - 26 * 60 * min, 3, false, false, true, ns, na, nr),
        )
        samples.forEach { encounterDao.insertNew(it) }
    }

    fun goTo(screen: Screen) { _screen.value = screen }

    fun confirmSas() {
        viewModelScope.launch { orchestrator?.confirmSas() }
    }

    fun loadFriends() {
        viewModelScope.launch { _friends.value = friendDao.all() }
    }

    fun stopProximity() {
        coordinator?.stop()
        coordinator = null
        proximityStarted = false
    }
}
