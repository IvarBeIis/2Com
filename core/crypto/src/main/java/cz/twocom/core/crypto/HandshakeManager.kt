package cz.twocom.core.crypto

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val HELLO_VERSION = 1
private const val MAX_FIELD_BYTES = 8192
private const val SIGNED_PREKEY_ID = 1
private const val KYBER_PREKEY_ID = 1
private const val MIN_ONE_TIME_PREKEYS = 10
private const val ONE_TIME_PREKEY_BATCH = 20
private const val SIGNED_PREKEY_ROTATION_DAYS = 7L
private const val DEVICE_ID = 1

/** Thrown for any malformed/oversized HELLO payload — caller must close the connection, never propagate to UI (invariant B.8). */
class HandshakeFormatException(message: String) : Exception(message)

/** Thrown when the peer's presented identity does not hash to the expected peerHash (FR-14 / invariant B.3). */
class PeerIdentityMismatchException(message: String) : Exception(message)

data class VerifiedHello(
    val peerHash: String,
    val signingPublicKey: ByteArray,
    val identityKey: IdentityKey,
    val bundle: PreKeyBundle,
)

/**
 * Owns X3DH handshake (HELLO exchange + PreKeyBundle processing) and Double Ratchet
 * encrypt/decrypt for established sessions, per ADR-0001 (libsignal-only, no separate
 * Noise layer).
 */
@Singleton
class HandshakeManager @Inject constructor(
    private val identityManager: IdentityManager,
    private val signalStore: SignalStore,
) {
    private fun addressFor(peerHash: String) = SignalProtocolAddress(peerHash, DEVICE_ID)

    /** Ensures this device has a fresh signed prekey, Kyber prekey, and a healthy one-time-prekey pool. */
    suspend fun ensurePrekeysReady() {
        signalStore.ensureLoaded()
        val identityKeyPair = signalStore.identityKeyPair
        val now = Instant.now()

        val needsSignedPreKey = try {
            val existing = signalStore.loadSignedPreKey(SIGNED_PREKEY_ID)
            Instant.ofEpochMilli(existing.timestamp).plus(SIGNED_PREKEY_ROTATION_DAYS, ChronoUnit.DAYS).isBefore(now)
        } catch (e: Exception) {
            true
        }
        if (needsSignedPreKey) {
            val ecKeyPair = ECKeyPair.generate()
            val sig = identityKeyPair.privateKey.calculateSignature(ecKeyPair.publicKey.serialize())
            signalStore.storeSignedPreKey(SIGNED_PREKEY_ID, SignedPreKeyRecord(SIGNED_PREKEY_ID, now.toEpochMilli(), ecKeyPair, sig))
        }

        val needsKyberPreKey = try {
            val existing = signalStore.loadKyberPreKey(KYBER_PREKEY_ID)
            Instant.ofEpochMilli(existing.timestamp).plus(SIGNED_PREKEY_ROTATION_DAYS, ChronoUnit.DAYS).isBefore(now)
        } catch (e: Exception) {
            true
        }
        if (needsKyberPreKey) {
            val kem = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
            val sig = identityKeyPair.privateKey.calculateSignature(kem.publicKey.serialize())
            signalStore.storeKyberPreKey(KYBER_PREKEY_ID, org.signal.libsignal.protocol.state.KyberPreKeyRecord(KYBER_PREKEY_ID, now.toEpochMilli(), kem, sig))
        }

        if (signalStore.availablePreKeyIds().size < MIN_ONE_TIME_PREKEYS) {
            var nextId = (signalStore.availablePreKeyIds().maxOrNull() ?: 0) + 1
            repeat(ONE_TIME_PREKEY_BATCH) {
                signalStore.storePreKey(nextId, PreKeyRecord(nextId, ECKeyPair.generate()))
                nextId++
            }
        }
    }

    /** Builds this device's current HELLO payload (own identity + one available prekey bundle). */
    suspend fun buildHelloPayload(): ByteArray {
        ensurePrekeysReady()
        val identity = identityManager.loadIdentity() ?: error("No identity generated yet")
        val signedPreKey = signalStore.loadSignedPreKey(SIGNED_PREKEY_ID)
        val kyberPreKey = signalStore.loadKyberPreKey(KYBER_PREKEY_ID)
        val oneTimePreKeyId = signalStore.availablePreKeyIds().minOrNull() ?: error("No one-time prekeys available")
        val oneTimePreKey = signalStore.loadPreKey(oneTimePreKeyId)
        val registrationId = signalStore.localRegistrationId

        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.writeByte(HELLO_VERSION)
            writeField(d, identity.signingPublicKey)
            writeField(d, identity.identityPublicKey)
            d.writeInt(registrationId)
            d.writeInt(SIGNED_PREKEY_ID)
            writeField(d, signedPreKey.keyPair.publicKey.serialize())
            writeField(d, signedPreKey.signature)
            d.writeInt(oneTimePreKeyId)
            writeField(d, oneTimePreKey.keyPair.publicKey.serialize())
            d.writeInt(KYBER_PREKEY_ID)
            writeField(d, kyberPreKey.keyPair.publicKey.serialize())
            writeField(d, kyberPreKey.signature)
        }
        return out.toByteArray()
    }

    /**
     * Parses a peer's HELLO payload and verifies BLAKE3(signingPub || identityPub) matches
     * [expectedPeerHash] (pass null only for an unsolicited inbound connection, where the
     * caller must instead check the resulting [VerifiedHello.peerHash] against its own
     * contact list before doing anything else with the connection).
     */
    fun parseAndVerifyHello(payload: ByteArray, expectedPeerHash: String?): VerifiedHello {
        try {
            val d = DataInputStream(ByteArrayInputStream(payload))
            val version = d.readUnsignedByte()
            if (version != HELLO_VERSION) throw HandshakeFormatException("Unsupported HELLO version $version")

            val signingPublicKey = readField(d)
            val identityKeyBytes = readField(d)
            val registrationId = d.readInt()
            val signedPreKeyId = d.readInt()
            val signedPreKeyPublicBytes = readField(d)
            val signedPreKeySignature = readField(d)
            val preKeyId = d.readInt()
            val preKeyPublicBytes = readField(d)
            val kyberPreKeyId = d.readInt()
            val kyberPreKeyPublicBytes = readField(d)
            val kyberPreKeySignature = readField(d)

            val peerHash = IdentityManager.identityHash(signingPublicKey, identityKeyBytes)
            if (expectedPeerHash != null && !peerHash.equals(expectedPeerHash, ignoreCase = true)) {
                throw PeerIdentityMismatchException("Presented identity hashes to $peerHash, expected $expectedPeerHash")
            }

            val identityKey = IdentityKey(identityKeyBytes)
            val bundle = PreKeyBundle(
                registrationId,
                DEVICE_ID,
                preKeyId,
                ECPublicKey(preKeyPublicBytes),
                signedPreKeyId,
                ECPublicKey(signedPreKeyPublicBytes),
                signedPreKeySignature,
                identityKey,
                kyberPreKeyId,
                KEMPublicKey(kyberPreKeyPublicBytes),
                kyberPreKeySignature,
            )
            return VerifiedHello(peerHash, signingPublicKey, identityKey, bundle)
        } catch (e: PeerIdentityMismatchException) {
            throw e
        } catch (e: HandshakeFormatException) {
            throw e
        } catch (e: EOFException) {
            throw HandshakeFormatException("Truncated HELLO payload")
        } catch (e: Exception) {
            throw HandshakeFormatException("Malformed HELLO payload: ${e.message}")
        }
    }

    suspend fun establishOutboundSession(peerHash: String, bundle: PreKeyBundle) {
        signalStore.ensureLoaded()
        SessionBuilder(signalStore, addressFor(peerHash)).process(bundle)
    }

    /** Returns (ciphertextType, bytes) — type is CiphertextMessage.PREKEY_TYPE or WHISPER_TYPE. */
    suspend fun encrypt(peerHash: String, plaintext: ByteArray): Pair<Int, ByteArray> {
        signalStore.ensureLoaded()
        val cipher = SessionCipher(signalStore, addressFor(peerHash))
        val message: CiphertextMessage = cipher.encrypt(plaintext)
        return message.type to message.serialize()
    }

    suspend fun decrypt(peerHash: String, ciphertextType: Int, bytes: ByteArray): ByteArray {
        signalStore.ensureLoaded()
        val cipher = SessionCipher(signalStore, addressFor(peerHash))
        return when (ciphertextType) {
            CiphertextMessage.PREKEY_TYPE -> cipher.decrypt(PreKeySignalMessage(bytes))
            CiphertextMessage.WHISPER_TYPE -> cipher.decrypt(SignalMessage(bytes))
            else -> throw HandshakeFormatException("Unknown ciphertext type $ciphertextType")
        }
    }

    private fun writeField(d: DataOutputStream, bytes: ByteArray) {
        require(bytes.size <= MAX_FIELD_BYTES) { "Field too large: ${bytes.size}" }
        d.writeShort(bytes.size)
        d.write(bytes)
    }

    private fun readField(d: DataInputStream): ByteArray {
        val len = d.readUnsignedShort()
        if (len > MAX_FIELD_BYTES) throw HandshakeFormatException("Field length $len exceeds max $MAX_FIELD_BYTES")
        val bytes = ByteArray(len)
        d.readFully(bytes)
        return bytes
    }
}
