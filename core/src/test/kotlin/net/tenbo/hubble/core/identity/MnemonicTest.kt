package net.tenbo.hubble.core.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MnemonicTest {
    @Test fun `generated phrase has 12 words and validates`() {
        val phrase = Mnemonic.generate(ByteArray(16) { 0 })
        assertEquals(12, phrase.trim().split(" ").size)
        assertTrue(Mnemonic.isValid(phrase))
    }

    @Test fun `same entropy yields same phrase yields same seed`() {
        val e = ByteArray(16) { 3 }
        val p1 = Mnemonic.generate(e)
        val p2 = Mnemonic.generate(e)
        assertEquals(p1, p2)
        assertEquals(Mnemonic.toSeed(p1).toList(), Mnemonic.toSeed(p2).toList())
    }

    @Test fun `tampered phrase fails validation`() {
        assertFalse(Mnemonic.isValid("not a real bip39 phrase at all nope nope nope"))
    }

    @Test fun `known BIP39 test vector matches the spec`() {
        // BIP39 reference vector: all-zero 128-bit entropy.
        val phrase = Mnemonic.generate(ByteArray(16) { 0 })
        assertEquals(
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about",
            phrase,
        )
        // Reference seed for that phrase with empty passphrase (BIP39 test vectors).
        val seedHex = Mnemonic.toSeed(phrase).joinToString("") { "%02x".format(it) }
        assertEquals(
            "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1" +
                "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",
            seedHex,
        )
    }
}
