package net.tenbo.hubble.core.identity

import net.tenbo.hubble.core.crypto.CryptoProvider

/** Deterministically derives an [Identity] from a BIP39 recovery phrase. */
class IdentityFactory(private val crypto: CryptoProvider) {

    fun fromPhrase(phrase: String): Identity {
        val seed = Mnemonic.toSeed(phrase) // 64 bytes
        // Independent 32-byte seeds via HKDF with distinct info labels.
        val signSeed = crypto.hkdf(seed, byteArrayOf(), "hubble-ed25519".toByteArray(), 32)
        val agreeSeed = crypto.hkdf(seed, byteArrayOf(), "hubble-x25519".toByteArray(), 32)
        val sign = crypto.generateEd25519(signSeed)
        val agree = crypto.generateX25519(agreeSeed)
        val fingerprint = crypto.blake2b(sign.publicKey, 8).toHex()
        return Identity(
            signingPublicKey = sign.publicKey,
            signingPrivateKey = sign.privateKey,
            agreementPublicKey = agree.publicKey,
            agreementPrivateKey = agree.privateKey,
            hubbleId = fingerprint,
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
