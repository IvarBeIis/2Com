package cz.twocom.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IdentityManagerTest {

    @Test
    fun `identityHash is 64 lowercase hex chars`() {
        val hash = IdentityManager.identityHash(ByteArray(32) { 1 }, ByteArray(33) { 2 })
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `identityHash is deterministic and input-sensitive`() {
        val a = IdentityManager.identityHash(ByteArray(32) { 1 }, ByteArray(33) { 2 })
        val b = IdentityManager.identityHash(ByteArray(32) { 1 }, ByteArray(33) { 2 })
        val c = IdentityManager.identityHash(ByteArray(32) { 3 }, ByteArray(33) { 2 })
        assertEquals(a, b)
        assertTrue(a != c)
    }

    @Test
    fun `identity display hash has spaces every 4 chars`() {
        val identity = Identity(
            signingPublicKey = ByteArray(32),
            identityPublicKey = ByteArray(33),
            identityHashHex = "abcd1234" + "0".repeat(56),
            createdAt = Instant.now(),
        )
        val display = identity.displayHash
        assertTrue(display.contains(" "))
        assertEquals(64 + 15, display.length) // 64 hex + 15 spaces
    }

    @Test
    fun `two identities with same hash are equal`() {
        val hash = "a".repeat(64)
        val id1 = Identity(ByteArray(32), ByteArray(33), hash, Instant.now())
        val id2 = Identity(ByteArray(32), ByteArray(33), hash, Instant.now())
        assertEquals(id1, id2)
    }
}
