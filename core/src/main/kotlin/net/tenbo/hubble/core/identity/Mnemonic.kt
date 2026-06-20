package net.tenbo.hubble.core.identity

import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import java.text.Normalizer

/**
 * BIP39 recovery phrases, implemented in-house against the official English
 * wordlist (no external dependency). Supports 12-word (128-bit entropy) phrases,
 * which is what Hubble generates.
 *
 *  - [generate]: entropy (16 bytes) -> 12 words, deterministically.
 *  - [isValid]:  checksum + wordlist validation.
 *  - [toSeed]:   PBKDF2-HMAC-SHA512(phrase, "mnemonic"+passphrase, 2048, 64 bytes).
 */
object Mnemonic {

    private val wordlist: List<String> by lazy {
        val stream = Mnemonic::class.java.getResourceAsStream("/bip39-english.txt")
            ?: error("bip39-english.txt resource missing")
        stream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .also { require(it.size == 2048) { "wordlist must have 2048 words, got ${it.size}" } }
    }

    private val wordIndex: Map<String, Int> by lazy {
        wordlist.withIndex().associate { (i, w) -> w to i }
    }

    /** Deterministically produce a 12-word phrase from 16 bytes of entropy. */
    fun generate(entropy: ByteArray): String {
        require(entropy.size == 16) { "expected 128 bits (16 bytes) of entropy" }
        val checksumBits = entropy.size * 8 / 32 // = 4 for 128-bit entropy
        val hash = sha256(entropy)
        // Bit string: entropy bits followed by the leading checksum bits.
        val bits = StringBuilder()
        for (b in entropy) bits.append(b.toUByte().toString(2).padStart(8, '0'))
        bits.append(hash[0].toUByte().toString(2).padStart(8, '0').substring(0, checksumBits))
        // 11 bits per word.
        return bits.chunked(11).joinToString(" ") { chunk ->
            wordlist[chunk.toInt(2)]
        }
    }

    /** Validate word membership, length, and the BIP39 checksum. */
    fun isValid(phrase: String): Boolean {
        val words = normalize(phrase).split(" ").filter { it.isNotEmpty() }
        if (words.size != 12) return false
        val indices = words.map { wordIndex[it] ?: return false }
        val bits = StringBuilder()
        for (idx in indices) bits.append(idx.toString(2).padStart(11, '0'))
        val total = bits.length            // 132
        val checksumBits = total / 33      // 4
        val entropyBits = total - checksumBits
        val entropy = ByteArray(entropyBits / 8) { i ->
            bits.substring(i * 8, i * 8 + 8).toInt(2).toByte()
        }
        val expected = sha256(entropy)[0].toUByte().toString(2)
            .padStart(8, '0').substring(0, checksumBits)
        return bits.substring(entropyBits) == expected
    }

    /** BIP39 seed: 64 bytes via PBKDF2-HMAC-SHA512, 2048 iterations. */
    fun toSeed(phrase: String, passphrase: String = ""): ByteArray {
        val pw = normalize(phrase).toCharArray()
        val salt = ("mnemonic" + normalize(passphrase)).toByteArray(Charsets.UTF_8)
        val gen = PKCS5S2ParametersGenerator(SHA512Digest())
        gen.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(pw), salt, 2048)
        return (gen.generateDerivedParameters(512) as KeyParameter).key
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.trim(), Normalizer.Form.NFKD)

    private fun sha256(data: ByteArray): ByteArray {
        val d = SHA256Digest()
        d.update(data, 0, data.size)
        return ByteArray(d.digestSize).also { d.doFinal(it, 0) }
    }
}
