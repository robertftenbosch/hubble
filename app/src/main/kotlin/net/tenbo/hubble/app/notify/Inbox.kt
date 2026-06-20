package net.tenbo.hubble.app.notify

import android.content.Context
import android.util.Log
import java.io.File
import net.tenbo.hubble.app.data.BlockDao
import net.tenbo.hubble.app.data.FriendDao
import net.tenbo.hubble.app.data.KeystoreVault
import net.tenbo.hubble.app.data.MatchDao
import net.tenbo.hubble.app.data.MessageDao
import net.tenbo.hubble.app.data.MessageEntity
import net.tenbo.hubble.app.data.PostDao
import net.tenbo.hubble.app.data.PostEntity
import net.tenbo.hubble.app.net.HubbleApi
import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.message.EnvelopeCodec
import net.tenbo.hubble.core.message.Incoming
import net.tenbo.hubble.core.message.Messaging

/**
 * Pulls our relay mailbox, decrypts every envelope, and persists what arrives — the same
 * path the in-app sync runs, but callable from the background worker. Returns the chat
 * messages that are genuinely new (sender not blocked) so the caller can post notifications.
 *
 * Posts are stored silently (they belong to the feed, not a notification); typing pings are
 * ignored in the background — they're ephemeral and there's no open chat to animate.
 */
object Inbox {

    data class NewMessage(val matchHubbleId: String, val name: String, val text: String)

    suspend fun poll(
        context: Context,
        vault: KeystoreVault,
        api: HubbleApi,
        friendDao: FriendDao,
        matchDao: MatchDao,
        messageDao: MessageDao,
        postDao: PostDao,
        blockDao: BlockDao,
    ): List<NewMessage> {
        val identity = vault.loadIdentity() ?: return emptyList()
        val crypto = BouncyCastleCrypto()
        val messaging = Messaging(crypto, identity)
        val blocked = blockDao.allIds().toSet()
        val matchByTag = matchDao.all().associateBy { messaging.expectedSenderTag(it.toFriendRecord()) }
        val friendByTag = friendDao.all().associateBy { messaging.expectedSenderTag(it.toRecord()) }

        val mailboxId = messaging.myMailboxId()
        val envelopes = runCatching { api.collect(mailboxId) }.getOrElse {
            Log.w("HubbleInbox", "collect failed: ${it.message}"); return emptyList()
        }
        val fresh = mutableListOf<NewMessage>()
        for (bytes in envelopes) {
            runCatching {
                val envelope = EnvelopeCodec.decode(bytes)
                val match = matchByTag[envelope.senderTag]
                val friend = friendByTag[envelope.senderTag]
                val record = match?.toFriendRecord() ?: friend?.toRecord() ?: return@runCatching
                if (record.hubbleId in blocked) return@runCatching
                when (val incoming = messaging.open(envelope, record)) {
                    is Incoming.Chat -> {
                        val matchId = match?.hubbleId ?: return@runCatching
                        val m = incoming.message
                        messageDao.insert(
                            MessageEntity(m.id, matchId, fromMe = false, text = m.text, sentAtEpochMs = m.sentAtMs),
                        )
                        matchDao.touch(matchId, m.sentAtMs)
                        fresh += NewMessage(matchId, match.name, m.text)
                    }
                    is Incoming.Voice -> {
                        val matchId = match?.hubbleId ?: return@runCatching
                        val v = incoming.message
                        val dir = File(context.filesDir, "voice").apply { mkdirs() }
                        val out = File(dir, "${v.id}.3gp").apply { writeBytes(v.audio) }
                        messageDao.insert(
                            MessageEntity(
                                v.id, matchId, fromMe = false, text = "Voice message", sentAtEpochMs = v.sentAtMs,
                                kind = "voice", audioPath = out.absolutePath, durationMs = v.durationMs,
                            ),
                        )
                        matchDao.touch(matchId, v.sentAtMs)
                        fresh += NewMessage(matchId, match.name, "🎤 Voice message")
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
                    }
                    is Incoming.Typing -> Unit // ephemeral; nothing to notify in the background
                }
            }
        }
        Log.i("HubbleInbox", "polled $mailboxId: ${envelopes.size} envelope(s), ${fresh.size} new message(s)")
        return fresh
    }
}
