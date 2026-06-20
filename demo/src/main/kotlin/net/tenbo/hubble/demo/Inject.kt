package net.tenbo.hubble.demo

import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.friend.FriendRecord
import net.tenbo.hubble.core.identity.Identity
import net.tenbo.hubble.core.message.ChatMessage
import net.tenbo.hubble.core.message.EnvelopeCodec
import net.tenbo.hubble.core.message.Messaging
import net.tenbo.hubble.core.message.VoiceMessage
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.UUID

/**
 * Test injector: seals a *real, decryptable* chat from a seeded demo match and deposits it
 * to a phone's relay mailbox, so we can verify the background notification renders end to end.
 *
 * Usage: ./gradlew :demo:inject -PmailboxId=<id> [-PbaseUrl=http://127.0.0.1:4000]
 *
 * It sends one message as each seeded match (Mara/Tycho/Noor). The phone only notifies for
 * matches that actually exist in its DB — the rest are dropped (sender unknown), exactly as
 * a real foreign envelope would be.
 */
fun main(args: Array<String>) {
    val argMap = args.filter { it.contains('=') }.associate { it.substringBefore('=') to it.substringAfter('=') }
    val mailboxId = argMap["mailboxId"] ?: error("pass -PmailboxId=<id>")
    val baseUrl = argMap["baseUrl"] ?: "http://127.0.0.1:4000"
    val kind = argMap["kind"] ?: "chat" // "chat" or "voice"
    // For voice we can supply a real recording via -PaudioFile=/path (so playback works);
    // otherwise we send placeholder bytes (delivery + notification + bubble still verify).
    val audioBytes = argMap["audioFile"]?.let { File(it).readBytes() } ?: ByteArray(1600) { (it % 251).toByte() }

    val crypto = BouncyCastleCrypto()
    // (seedByte, name, text) — mirrors HubbleViewModel.seedSampleEncounters.
    val people = listOf(
        Triple(10.toByte(), "Mara", "Hey! Hubble pinged — funny seeing your name pop up. Coffee at Léon again?"),
        Triple(11.toByte(), "Tycho", "Still owe you that synth demo. You around this week?"),
        Triple(12.toByte(), "Noor", "Found a poetry margin you'd absolutely judge. In a good way."),
    )

    for ((seed, name, text) in people) {
        val s = ByteArray(32) { seed }
        val sign = crypto.generateEd25519(s)
        val agree = crypto.generateX25519(s)
        val rootKey = crypto.hkdf(s, ByteArray(0), "demo-root".toByteArray(), 32)
        val hubbleId = "demo-${name.lowercase()}"

        val sender = Identity(sign.publicKey, sign.privateKey, agree.publicKey, agree.privateKey, hubbleId)
        // recipient record: only rootKey matters for encryption; recipientMailbox is ignored
        // (we POST straight to the phone's mailbox id, which is how the relay is addressed).
        val recipient = FriendRecord(hubbleId, name, sign.publicKey, agree.publicKey, rootKey, 0L)

        val now = System.currentTimeMillis()
        val messaging = Messaging(crypto, sender)
        val envelope = if (kind == "voice") {
            val voice = VoiceMessage(UUID.randomUUID().toString(), hubbleId, "audio/3gpp", 4000, now, audioBytes)
            messaging.sealVoice(voice, recipient)
        } else {
            messaging.sealChat(ChatMessage(UUID.randomUUID().toString(), hubbleId, text, now), recipient)
        }
        val body = EnvelopeCodec.encode(envelope)
        deposit(baseUrl, mailboxId, body)
        println("deposited $kind from $name -> mailbox $mailboxId (${body.size} bytes)")
    }
}

private fun deposit(baseUrl: String, mailboxId: String, envelope: ByteArray) {
    val json = """{"envelope":"${Base64.getEncoder().encodeToString(envelope)}"}"""
    val conn = (URL("$baseUrl/mailbox/$mailboxId").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }
    conn.outputStream.use { it.write(json.toByteArray()) }
    check(conn.responseCode in 200..299) { "deposit failed: ${conn.responseCode}" }
    conn.disconnect()
}
