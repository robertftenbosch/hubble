package net.tenbo.hubble.core.crypto

/**
 * Derives a Short Authentication String from a handshake transcript.
 * Both co-present users compare the emoji aloud to defeat BLE relay/MITM.
 */
class Sas(private val crypto: CryptoProvider, private val count: Int = 5) {

    // A 64-emoji alphabet (6 bits each). Index = byte and 0x3F.
    private val alphabet: List<String> = run {
        val s = "😀😁😂🤣😊😍😎🤩🥳🤔🙃😴🤯🥺😇🤠" +
            "🐶🐱🦊🐻🐼🐨🦁🐯🦄🐲🐙🦋🐝🐞🦀🐬" +
            "🍎🍌🍓🍇🍉🍒🥑🌶🌽🥕🍔🍕🌮🍩🍪🎂" +
            "⚽🏀🎸🎲🚀✈🚲⛵🎈🎁🔑🔔💎🌈⭐🔥"
        val cps = s.codePoints().toArray()
        require(cps.size >= 64) { "alphabet must have >= 64 emoji, got ${cps.size}" }
        cps.take(64).map { String(Character.toChars(it)) }
    }

    fun emoji(transcript: ByteArray): List<String> {
        val digest = crypto.blake2b(transcript, count) // one byte per emoji
        return digest.map { alphabet[it.toInt() and 0x3F] }
    }
}
