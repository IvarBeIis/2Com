package cz.twocom.core.crypto

import java.time.Instant

/**
 * [signingPublicKey] (Ed25519) authenticates announce/control messages.
 * [identityPublicKey] is the serialized libsignal Curve25519 identity key used for X3DH.
 * identityHashHex = BLAKE3(signingPublicKey || identityPublicKey) is the app's peer address.
 */
data class Identity(
    val signingPublicKey: ByteArray,
    val identityPublicKey: ByteArray,
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
