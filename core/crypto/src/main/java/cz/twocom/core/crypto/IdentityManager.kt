package cz.twocom.core.crypto

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.identityDataStore by preferencesDataStore("identity")

@Singleton
class IdentityManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keySigningPriv = stringPreferencesKey("signing_priv")
    private val keySigningPub = stringPreferencesKey("signing_pub")
    private val keyAgreementPriv = stringPreferencesKey("agreement_priv")
    private val keyAgreementPub = stringPreferencesKey("agreement_pub")
    private val keyCreatedAt = stringPreferencesKey("created_at")

    suspend fun generateIdentity(): Identity {
        val rng = SecureRandom()

        val signingGen = Ed25519KeyPairGenerator()
        signingGen.init(Ed25519KeyGenerationParameters(rng))
        val signingPair = signingGen.generateKeyPair()
        val signingPrivBytes = ByteArray(32).also {
            (signingPair.private as org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters).encode(it, 0)
        }
        val signingPubBytes = ByteArray(32).also {
            (signingPair.public as Ed25519PublicKeyParameters).encode(it, 0)
        }

        val agreementGen = X25519KeyPairGenerator()
        agreementGen.init(X25519KeyGenerationParameters(rng))
        val agreementPair = agreementGen.generateKeyPair()
        val agreementPrivBytes = ByteArray(32).also {
            (agreementPair.private as org.bouncycastle.crypto.params.X25519PrivateKeyParameters).encode(it, 0)
        }
        val agreementPubBytes = ByteArray(32).also {
            (agreementPair.public as X25519PublicKeyParameters).encode(it, 0)
        }

        val hash = blake3(signingPubBytes + agreementPubBytes)
        val hashHex = hash.toHexString()
        val now = Instant.now()

        context.identityDataStore.edit { prefs ->
            prefs[keySigningPriv] = signingPrivBytes.toHexString()
            prefs[keySigningPub] = signingPubBytes.toHexString()
            prefs[keyAgreementPriv] = agreementPrivBytes.toHexString()
            prefs[keyAgreementPub] = agreementPubBytes.toHexString()
            prefs[keyCreatedAt] = now.toString()
        }

        return Identity(
            signingPublicKey = signingPubBytes,
            agreementPublicKey = agreementPubBytes,
            identityHashHex = hashHex,
            createdAt = now,
        )
    }

    suspend fun loadIdentity(): Identity? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        val signingPub = prefs[keySigningPub]?.hexToBytes() ?: return null
        val agreementPub = prefs[keyAgreementPub]?.hexToBytes() ?: return null
        val createdAt = prefs[keyCreatedAt]?.let { Instant.parse(it) } ?: return null
        val hash = blake3(signingPub + agreementPub)
        return Identity(
            signingPublicKey = signingPub,
            agreementPublicKey = agreementPub,
            identityHashHex = hash.toHexString(),
            createdAt = createdAt,
        )
    }

    suspend fun loadSigningPrivateKey(): ByteArray? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        return prefs[keySigningPriv]?.hexToBytes()
    }

    suspend fun loadAgreementPrivateKey(): ByteArray? {
        val prefs = context.identityDataStore.data.firstOrNull() ?: return null
        return prefs[keyAgreementPriv]?.hexToBytes()
    }

    private fun blake3(input: ByteArray): ByteArray {
        val digest = org.bouncycastle.crypto.digests.Blake3Digest(256)
        digest.update(input, 0, input.size)
        val out = ByteArray(32)
        digest.doFinal(out, 0)
        return out
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
