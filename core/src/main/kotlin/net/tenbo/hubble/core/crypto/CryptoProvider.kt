package net.tenbo.hubble.core.crypto

/** A keypair as raw bytes. */
data class KeyPairBytes(val publicKey: ByteArray, val privateKey: ByteArray)

/** Transport- and platform-agnostic crypto operations used by the handshake. */
interface CryptoProvider {
    /** Deterministic X25519 keypair from a 32-byte seed. */
    fun generateX25519(seed: ByteArray): KeyPairBytes

    /** Deterministic Ed25519 keypair from a 32-byte seed. */
    fun generateEd25519(seed: ByteArray): KeyPairBytes

    fun x25519SharedSecret(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray

    fun sign(privateKey: ByteArray, message: ByteArray): ByteArray

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean

    /** HKDF-SHA256. */
    fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray

    /** BLAKE2b digest, used for fingerprints and the SAS transcript hash. */
    fun blake2b(data: ByteArray, length: Int): ByteArray
}
