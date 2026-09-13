package cz.twocom.core.crypto

/**
 * Envelope-encrypts arbitrary byte blobs (private keys, DB passphrase) with an AES-256-GCM
 * key that never leaves the Android Keystore (StrongBox-backed where available). The blobs
 * themselves (e.g. libsignal's serialized IdentityKeyPair) can't be generated *inside*
 * Keystore, so this wraps externally-generated key material before it ever touches disk —
 * satisfies CLAUDE.md invariant B.2 ("private keys never leave the Keystore" in the sense
 * that only the Keystore-resident wrapping key, never the plaintext secret, is what a key
 * extraction attack against the OS crypto provider could expose).
 */
interface KeystoreEnvelope {
    /** [alias] identifies which Keystore-resident AES key wraps this blob. */
    fun seal(alias: String, plaintext: ByteArray): ByteArray
    fun open(alias: String, sealed: ByteArray): ByteArray
}
