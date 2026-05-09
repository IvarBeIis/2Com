package cz.twocom.core.crypto

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageCipher @Inject constructor() {

    private val rng = SecureRandom()

    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val nonce = ByteArray(12).also { rng.nextBytes(it) }
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(true, AEADParameters(KeyParameter(key), 128, nonce))
        val output = ByteArray(cipher.getOutputSize(plaintext.size))
        val len = cipher.processBytes(plaintext, 0, plaintext.size, output, 0)
        cipher.doFinal(output, len)
        return nonce + output
    }

    fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray {
        require(ciphertext.size > 12) { "Ciphertext too short" }
        val nonce = ciphertext.copyOf(12)
        val payload = ciphertext.copyOfRange(12, ciphertext.size)
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(false, AEADParameters(KeyParameter(key), 128, nonce))
        val output = ByteArray(cipher.getOutputSize(payload.size))
        val len = cipher.processBytes(payload, 0, payload.size, output, 0)
        cipher.doFinal(output, len)
        return output
    }
}
