package cz.twocom.core.crypto

import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Exercises HandshakeManager.parseAndVerifyHello directly — the function that enforces
 * FR-14 (peer hash must match the identity key actually presented at handshake time,
 * invariant B.3: reject on mismatch, no fallback). Constructed with mocks because this
 * function touches neither IdentityManager nor SignalStore.
 */
class HandshakeFramingTest {

    private val manager = HandshakeManager(mockk(relaxed = true), mockk(relaxed = true))

    private fun writeField(d: DataOutputStream, bytes: ByteArray) {
        d.writeShort(bytes.size)
        d.write(bytes)
    }

    @Test
    fun `verified hello returns the hash of the presented keys`() {
        val signingPub = ByteArray(32) { 9 }
        val identityKeyPair = IdentityKeyPair.generate()
        val identityPub = identityKeyPair.publicKey.serialize()
        val expectedHash = IdentityManager.identityHash(signingPub, identityPub)

        val payload = encodeHelloWithIdentity(signingPub, identityKeyPair)
        val result = manager.parseAndVerifyHello(payload, expectedPeerHash = expectedHash)

        assertEquals(expectedHash, result.peerHash)
        assertArrayEquals(signingPub, result.signingPublicKey)
    }

    @Test
    fun `mismatched peer hash is rejected with no fallback`() {
        val signingPub = ByteArray(32) { 9 }
        val identityKeyPair = IdentityKeyPair.generate()
        val payload = encodeHelloWithIdentity(signingPub, identityKeyPair)

        assertThrows(PeerIdentityMismatchException::class.java) {
            manager.parseAndVerifyHello(payload, expectedPeerHash = "f".repeat(64))
        }
    }

    @Test
    fun `truncated payload is rejected as malformed, not propagated as a crash`() {
        val truncated = ByteArray(3)
        assertThrows(HandshakeFormatException::class.java) {
            manager.parseAndVerifyHello(truncated, expectedPeerHash = null)
        }
    }

    private fun encodeHelloWithIdentity(signingPub: ByteArray, identityKeyPair: IdentityKeyPair): ByteArray {
        val signedPreKey = ECKeyPair.generate()
        val signedPreKeySig = identityKeyPair.privateKey.calculateSignature(signedPreKey.publicKey.serialize())
        val oneTimePreKey = ECKeyPair.generate()
        val kyber = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberSig = identityKeyPair.privateKey.calculateSignature(kyber.publicKey.serialize())

        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.writeByte(1)
            writeField(d, signingPub)
            writeField(d, identityKeyPair.publicKey.serialize())
            d.writeInt(42)
            d.writeInt(1)
            writeField(d, signedPreKey.publicKey.serialize())
            writeField(d, signedPreKeySig)
            d.writeInt(7)
            writeField(d, oneTimePreKey.publicKey.serialize())
            d.writeInt(1)
            writeField(d, kyber.publicKey.serialize())
            writeField(d, kyberSig)
        }
        return out.toByteArray()
    }
}
