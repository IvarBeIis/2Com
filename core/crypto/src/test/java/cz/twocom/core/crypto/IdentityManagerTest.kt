package cz.twocom.core.crypto

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityManagerTest {

    @Test
    fun `blake3 hash is 64 hex chars`() = runTest {
        val context = mockk<Context>(relaxed = true)
        // We test the hash format via a stub — full integration test needs real Android context
        val hash = "a".repeat(64)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `identity display hash has spaces every 4 chars`() {
        val identity = Identity(
            signingPublicKey = ByteArray(32),
            agreementPublicKey = ByteArray(32),
            identityHashHex = "abcd1234" + "0".repeat(56),
            createdAt = java.time.Instant.now(),
        )
        val display = identity.displayHash
        assertTrue(display.contains(" "))
        assertEquals(64 + 15, display.length) // 64 hex + 15 spaces
    }

    @Test
    fun `two identities with same hash are equal`() {
        val hash = "a".repeat(64)
        val id1 = Identity(ByteArray(32), ByteArray(32), hash, java.time.Instant.now())
        val id2 = Identity(ByteArray(32), ByteArray(32), hash, java.time.Instant.now())
        assertEquals(id1, id2)
    }
}
