package cz.twocom.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128
private const val GCM_NONCE_BYTES = 12

@Singleton
class AndroidKeystoreEnvelope @Inject constructor() : KeystoreEnvelope {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val rng = SecureRandom()

    override fun seal(alias: String, plaintext: ByteArray): ByteArray {
        val key = wrappingKey(alias)
        val nonce = ByteArray(GCM_NONCE_BYTES).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(plaintext)
        return nonce + ciphertext
    }

    override fun open(alias: String, sealed: ByteArray): ByteArray {
        require(sealed.size > GCM_NONCE_BYTES) { "Sealed blob too short" }
        val key = wrappingKey(alias)
        val nonce = sealed.copyOf(GCM_NONCE_BYTES)
        val ciphertext = sealed.copyOfRange(GCM_NONCE_BYTES, sealed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(ciphertext)
    }

    private fun wrappingKey(alias: String): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return generateWrappingKey(alias)
    }

    private fun generateWrappingKey(alias: String): SecretKey {
        val purpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val baseSpec = KeyGenParameterSpec.Builder(alias, purpose)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        return try {
            generator.init(baseSpec.setIsStrongBoxBacked(true).build())
            generator.generateKey()
        } catch (e: Exception) {
            // StrongBox unavailable on this device — fall back to the TEE-backed key.
            generator.init(baseSpec.setIsStrongBoxBacked(false).build())
            generator.generateKey()
        }
    }
}
