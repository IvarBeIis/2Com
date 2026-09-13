package cz.twocom.core.crypto

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import org.signal.libsignal.protocol.IdentityKeyPair
import java.security.SecureRandom
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.identityDataStore by preferencesDataStore("identity")
private const val KEYSTORE_ALIAS_IDENTITY = "identity_wrap_v1"
private const val MAX_REGISTRATION_ID = 16380

@Singleton
class IdentityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val envelope: KeystoreEnvelope,
) {
    private val keySigningPrivSealed = stringPreferencesKey("signing_priv_sealed")
    private val keySigningPub = stringPreferencesKey("signing_pub")
    private val keyIdentitySealed = stringPreferencesKey("identity_keypair_sealed")
    private val keyRegistrationId = intPreferencesKey("registration_id")
    private val keyCreatedAt = stringPreferencesKey("created_at")

    suspend fun generateIdentity(): Identity {
        val rng = SecureRandom()

        val signingGen = org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator()
        signingGen.init(org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters(rng))
        val signingPair = signingGen.generateKeyPair()
        val signingPrivBytes = ByteArray(32).also {
            (signingPair.private as org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters).encode(it, 0)
        }
        val signingPubBytes = ByteArray(32).also {
            (signingPair.public as org.bouncycastle.crypto.params.Ed25519PublicKeyParameters).encode(it, 0)
        }

        val identityKeyPair = IdentityKeyPair.generate()
        val registrationId = rng.nextInt(MAX_REGISTRATION_ID) + 1
        val now = Instant.now()

        context.identityDataStore.edit { prefs ->
            prefs[keySigningPrivSealed] = envelope.seal(KEYSTORE_ALIAS_IDENTITY, signingPrivBytes).toHexString()
            prefs[keySigningPub] = signingPubBytes.toHexString()
            prefs[keyIdentitySealed] =
                envelope.seal(KEYSTORE_ALIAS_IDENTITY, identityKeyPair.serialize()).toHexString()
            prefs[keyRegistrationId] = registrationId
            prefs[keyCreatedAt] = now.toString()
        }

        return Identity(
            signingPublicKey = signingPubBytes,
            identityPublicKey = identityKeyPair.publicKey.serialize(),
            identityHashHex = identityHash(signingPubBytes, identityKeyPair.publicKey.serialize()),
            createdAt = now,
        )
    }

    suspend fun loadIdentity(): Identity? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        val signingPub = prefs[keySigningPub]?.hexToBytes() ?: return null
        val identityKeyPair = loadIdentityKeyPair() ?: return null
        val createdAt = prefs[keyCreatedAt]?.let { Instant.parse(it) } ?: return null
        val identityPub = identityKeyPair.publicKey.serialize()
        return Identity(
            signingPublicKey = signingPub,
            identityPublicKey = identityPub,
            identityHashHex = identityHash(signingPub, identityPub),
            createdAt = createdAt,
        )
    }

    suspend fun loadSigningPrivateKey(): ByteArray? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        val sealed = prefs[keySigningPrivSealed]?.hexToBytes() ?: return null
        return envelope.open(KEYSTORE_ALIAS_IDENTITY, sealed)
    }

    suspend fun loadIdentityKeyPair(): IdentityKeyPair? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        val sealed = prefs[keyIdentitySealed]?.hexToBytes() ?: return null
        return IdentityKeyPair(envelope.open(KEYSTORE_ALIAS_IDENTITY, sealed))
    }

    suspend fun loadRegistrationId(): Int? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        return prefs[keyRegistrationId]
    }

    companion object {
        /** BLAKE3(signingPublicKey || identityPublicKey) — this app's peer address / hex hash. */
        fun identityHash(signingPublicKey: ByteArray, identityPublicKey: ByteArray): String =
            blake3(signingPublicKey + identityPublicKey).toHexString()

        private fun blake3(input: ByteArray): ByteArray {
            val digest = org.bouncycastle.crypto.digests.Blake3Digest(256)
            digest.update(input, 0, input.size)
            val out = ByteArray(32)
            digest.doFinal(out, 0)
            return out
        }
    }
}

internal fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
internal fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
