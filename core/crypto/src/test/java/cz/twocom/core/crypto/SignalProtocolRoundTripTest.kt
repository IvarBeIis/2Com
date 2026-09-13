package cz.twocom.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore

/**
 * Proves the X3DH handshake + Double Ratchet round trip actually works end to end with real
 * libsignal APIs (audit recommendation #8: an integration test that would have caught
 * "encryption never wired up"). Uses libsignal's own InMemorySignalProtocolStore rather than
 * this app's Room-backed SignalStore, since Room needs an Android/instrumented environment —
 * SocketHandshake/HandshakeManager wire this same API into the app's persistent store.
 */
class SignalProtocolRoundTripTest {

    private fun bundleFor(store: InMemorySignalProtocolStore, deviceId: Int): PreKeyBundle {
        val signedPreKey = ECKeyPair.generate()
        val signedSig = store.identityKeyPair.privateKey.calculateSignature(signedPreKey.publicKey.serialize())
        store.storeSignedPreKey(1, SignedPreKeyRecord(1, 0, signedPreKey, signedSig))

        val oneTime = ECKeyPair.generate()
        store.storePreKey(1, PreKeyRecord(1, oneTime))

        val kyber = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberSig = store.identityKeyPair.privateKey.calculateSignature(kyber.publicKey.serialize())
        store.storeKyberPreKey(1, org.signal.libsignal.protocol.state.KyberPreKeyRecord(1, 0, kyber, kyberSig))

        return PreKeyBundle(
            store.localRegistrationId,
            deviceId,
            1,
            oneTime.publicKey,
            1,
            signedPreKey.publicKey,
            signedSig,
            store.identityKeyPair.publicKey,
            1,
            kyber.publicKey,
            kyberSig,
        )
    }

    @Test
    fun `alice and bob exchange an encrypted message via X3DH + Double Ratchet`() {
        val aliceStore = InMemorySignalProtocolStore(IdentityKeyPair.generate(), 1)
        val bobStore = InMemorySignalProtocolStore(IdentityKeyPair.generate(), 2)

        val aliceAddress = SignalProtocolAddress("alice", 1)
        val bobAddress = SignalProtocolAddress("bob", 1)

        // Alice processes Bob's bundle (this IS the FR-14-verified handshake in the real app —
        // HandshakeManager checks the identity hash before ever reaching this call).
        SessionBuilder(aliceStore, bobAddress).process(bundleFor(bobStore, 1))

        val aliceCipher = SessionCipher(aliceStore, bobAddress)
        val plaintext = "Hello, 2Com!".toByteArray(Charsets.UTF_8)
        val ciphertext = aliceCipher.encrypt(plaintext)

        val bobCipher = SessionCipher(bobStore, aliceAddress)
        val decrypted = when (ciphertext.type) {
            CiphertextMessage.PREKEY_TYPE -> bobCipher.decrypt(PreKeySignalMessage(ciphertext.serialize()))
            else -> bobCipher.decrypt(SignalMessage(ciphertext.serialize()))
        }

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `garbled ciphertext does not decrypt to valid plaintext`() {
        val aliceStore = InMemorySignalProtocolStore(IdentityKeyPair.generate(), 1)
        val bobStore = InMemorySignalProtocolStore(IdentityKeyPair.generate(), 2)
        val bobAddress = SignalProtocolAddress("bob", 1)
        val aliceAddress = SignalProtocolAddress("alice", 1)

        SessionBuilder(aliceStore, bobAddress).process(bundleFor(bobStore, 1))
        val ciphertext = SessionCipher(aliceStore, bobAddress).encrypt("hi".toByteArray())
        val corrupted = ciphertext.serialize().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }

        org.junit.Assert.assertThrows(Exception::class.java) {
            SessionCipher(bobStore, aliceAddress).decrypt(PreKeySignalMessage(corrupted))
        }
    }
}
