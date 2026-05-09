package cz.twocom.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.SecureRandom

class MessageCipherTest {

    private val cipher = MessageCipher()
    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Test
    fun `encrypt then decrypt returns original`() {
        val plaintext = "Hello, 2Com!".toByteArray()
        val encrypted = cipher.encrypt(plaintext, key)
        val decrypted = cipher.decrypt(encrypted, key)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypted output is different from plaintext`() {
        val plaintext = "Test message".toByteArray()
        val encrypted = cipher.encrypt(plaintext, key)
        assertNotEquals(plaintext.toList(), encrypted.toList())
    }

    @Test
    fun `two encryptions of same plaintext differ (random nonce)`() {
        val plaintext = "Same text".toByteArray()
        val enc1 = cipher.encrypt(plaintext, key)
        val enc2 = cipher.encrypt(plaintext, key)
        assertNotEquals(enc1.toList(), enc2.toList())
    }

    @Test
    fun `ciphertext is longer than plaintext (nonce + tag overhead)`() {
        val plaintext = ByteArray(32)
        val encrypted = cipher.encrypt(plaintext, key)
        assertEquals(true, encrypted.size > plaintext.size + 12) // nonce 12 + GCM tag 16
    }
}
