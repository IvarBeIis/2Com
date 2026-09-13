package cz.twocom.core.crypto

import cz.twocom.core.crypto.store.KyberPreKeyRow
import cz.twocom.core.crypto.store.PreKeyRow
import cz.twocom.core.crypto.store.SessionRow
import cz.twocom.core.crypto.store.SignalStoreDao
import cz.twocom.core.crypto.store.SignedPreKeyRow
import cz.twocom.core.crypto.store.TrustedIdentityRow
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.ReusedBaseKeyException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun SignalProtocolAddress.key() = "${name}:$deviceId"

/**
 * Persistent [SignalProtocolStore] backing libsignal's X3DH + Double Ratchet sessions.
 * Prekey/session/session-identity material lives in the SQLCipher-encrypted [SignalDatabase]
 * (core/crypto/store) — the own identity key pair itself is loaded once from
 * [IdentityManager], which keeps it envelope-encrypted via [KeystoreEnvelope] on top of that.
 *
 * SenderKeyStore (group messaging) is intentionally unsupported: 1:1 only is in MVP scope
 * (CLAUDE.md C.0 / doc 02_PRD).
 */
@Singleton
class SignalStore @Inject constructor(
    private val dao: SignalStoreDao,
    private val identityManager: IdentityManager,
) : SignalProtocolStore {

    private var cachedIdentityKeyPair: IdentityKeyPair? = null
    private var cachedRegistrationId: Int? = null

    /** Must be called (idempotent) before this store is used to build a SessionBuilder/Cipher. */
    suspend fun ensureLoaded() {
        if (cachedIdentityKeyPair != null) return
        cachedIdentityKeyPair = identityManager.loadIdentityKeyPair()
            ?: error("No identity generated yet")
        cachedRegistrationId = identityManager.loadRegistrationId()
            ?: error("No identity generated yet")
    }

    // --- IdentityKeyStore ---

    override fun getIdentityKeyPair(): IdentityKeyPair =
        cachedIdentityKeyPair ?: error("SignalStore.ensureLoaded() was not called")

    override fun getLocalRegistrationId(): Int =
        cachedRegistrationId ?: error("SignalStore.ensureLoaded() was not called")

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): IdentityKeyStore.IdentityChange {
        val existing = dao.findTrustedIdentity(address.key())
        dao.putTrustedIdentity(TrustedIdentityRow(address.key(), identityKey.serialize()))
        return if (existing != null && !existing.identityKeyBytes.contentEquals(identityKey.serialize())) {
            IdentityKeyStore.IdentityChange.REPLACED_EXISTING
        } else {
            IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
        }
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction,
    ): Boolean {
        // Trust-on-first-use at the libsignal layer; the actual authenticity check (peer hash
        // == BLAKE3(signing || identity pub)) happens in HandshakeManager before this is ever
        // reached — a mismatch there closes the connection and this method is never called
        // for that peer (CLAUDE.md invariant B.3 / FR-14).
        val existing = dao.findTrustedIdentity(address.key()) ?: return true
        return existing.identityKeyBytes.contentEquals(identityKey.serialize())
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? =
        dao.findTrustedIdentity(address.key())?.let { IdentityKey(it.identityKeyBytes) }

    // --- PreKeyStore ---

    override fun loadPreKey(preKeyId: Int): PreKeyRecord =
        dao.findPreKey(preKeyId)?.let { PreKeyRecord(it.data) }
            ?: throw InvalidKeyIdException("No such prekey: $preKeyId")

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        dao.putPreKey(PreKeyRow(preKeyId, record.serialize()))
    }

    override fun containsPreKey(preKeyId: Int): Boolean = dao.findPreKey(preKeyId) != null

    override fun removePreKey(preKeyId: Int) {
        dao.deletePreKey(preKeyId)
    }

    // --- SignedPreKeyStore ---

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord =
        dao.findSignedPreKey(signedPreKeyId)?.let { SignedPreKeyRecord(it.data) }
            ?: throw InvalidKeyIdException("No such signed prekey: $signedPreKeyId")

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> =
        dao.allSignedPreKeys().map { SignedPreKeyRecord(it.data) }.toMutableList()

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        dao.putSignedPreKey(SignedPreKeyRow(signedPreKeyId, record.serialize()))
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean =
        dao.findSignedPreKey(signedPreKeyId) != null

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        dao.deleteSignedPreKey(signedPreKeyId)
    }

    // --- KyberPreKeyStore ---

    override fun loadKyberPreKey(kyberPreKeyId: Int): KyberPreKeyRecord =
        dao.findKyberPreKey(kyberPreKeyId)?.let { KyberPreKeyRecord(it.data) }
            ?: throw InvalidKeyIdException("No such kyber prekey: $kyberPreKeyId")

    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> =
        dao.allKyberPreKeys().map { KyberPreKeyRecord(it.data) }.toMutableList()

    override fun storeKyberPreKey(kyberPreKeyId: Int, record: KyberPreKeyRecord) {
        dao.putKyberPreKey(KyberPreKeyRow(kyberPreKeyId, record.serialize()))
    }

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean =
        dao.findKyberPreKey(kyberPreKeyId) != null

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey) {
        val row = dao.findKyberPreKey(kyberPreKeyId)
        if (row != null && row.used) {
            throw ReusedBaseKeyException()
        }
        dao.markKyberPreKeyUsed(kyberPreKeyId)
    }

    fun hasKyberPreKeyBeenUsed(kyberPreKeyId: Int): Boolean =
        dao.findKyberPreKey(kyberPreKeyId)?.used ?: false

    fun availablePreKeyIds(): List<Int> = dao.allPreKeyIds()

    // --- SessionStore ---

    override fun loadSession(address: SignalProtocolAddress): SessionRecord =
        dao.findSession(address.key())?.let { SessionRecord(it.data) } ?: SessionRecord()

    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> =
        addresses.mapNotNull { dao.findSession(it.key())?.let { row -> SessionRecord(row.data) } }.toMutableList()

    override fun getSubDeviceSessions(name: String): MutableList<Int> =
        dao.sessionAddressesForName(name).mapNotNull { it.substringAfterLast(':').toIntOrNull() }.toMutableList()

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        dao.putSession(SessionRow(address.key(), record.serialize()))
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean = dao.findSession(address.key()) != null

    override fun deleteSession(address: SignalProtocolAddress) {
        dao.deleteSession(address.key())
    }

    override fun deleteAllSessions(name: String) {
        dao.deleteAllSessionsForName(name)
    }

    // --- SenderKeyStore (group messaging — out of MVP scope) ---

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
        throw UnsupportedOperationException("Group messaging is out of MVP scope")
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? =
        throw UnsupportedOperationException("Group messaging is out of MVP scope")
}
