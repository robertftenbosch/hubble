package net.tenbo.hubble.core.crypto

import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.math.ec.rfc7748.X25519

/** Pure-Java crypto via Bouncy Castle — identical behaviour on the JVM and Android. */
class BouncyCastleCrypto : CryptoProvider {

    override fun generateX25519(seed: ByteArray): KeyPairBytes {
        require(seed.size >= X25519.SCALAR_SIZE) { "seed too short" }
        val priv = X25519PrivateKeyParameters(seed, 0)
        return KeyPairBytes(priv.generatePublicKey().encoded, priv.encoded)
    }

    override fun generateEd25519(seed: ByteArray): KeyPairBytes {
        require(seed.size >= 32) { "seed too short" }
        val priv = Ed25519PrivateKeyParameters(seed, 0)
        return KeyPairBytes(priv.generatePublicKey().encoded, priv.encoded)
    }

    override fun x25519SharedSecret(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        val out = ByteArray(X25519.POINT_SIZE)
        X25519.calculateAgreement(privateKey, 0, peerPublicKey, 0, out, 0)
        return out
    }

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        val params = Ed25519PrivateKeyParameters(privateKey, 0)
        val signer = Ed25519Signer().apply { init(true, params); update(message, 0, message.size) }
        return signer.generateSignature()
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        val params = Ed25519PublicKeyParameters(publicKey, 0)
        val signer = Ed25519Signer().apply { init(false, params); update(message, 0, message.size) }
        return signer.verifySignature(signature)
    }

    override fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val gen = HKDFBytesGenerator(SHA256Digest())
        gen.init(HKDFParameters(ikm, salt, info))
        return ByteArray(length).also { gen.generateBytes(it, 0, length) }
    }

    override fun blake2b(data: ByteArray, length: Int): ByteArray {
        val d = Blake2bDigest(length * 8)
        d.update(data, 0, data.size)
        return ByteArray(length).also { d.doFinal(it, 0) }
    }
}
