package cz.twocom.core.crypto

import java.time.Instant

data class Identity(
    val signingPublicKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val identityHashHex: String,
    val createdAt: Instant,
) {
    val displayHash: String
        get() = identityHashHex.chunked(4).joinToString(" ")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Identity) return false
        return identityHashHex == other.identityHashHex
    }

    override fun hashCode(): Int = identityHashHex.hashCode()
}
