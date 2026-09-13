package cz.twocom.core.transport

import android.util.Log
import cz.twocom.core.crypto.HandshakeFormatException
import cz.twocom.core.crypto.HandshakeManager
import cz.twocom.core.crypto.PeerIdentityMismatchException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

private const val TAG = "SocketHandshake"

/**
 * Performs the HELLO exchange over an already-open [socket] (both sides send their own HELLO
 * immediately, then read the peer's), verifies the peer's identity, and — only on success —
 * builds a [Connection] backed by a background frame-reading/decrypting loop.
 *
 * [expectedPeerHash] is the hash we dialed (outbound) or null for an unsolicited inbound
 * socket. [onVerified] lets the caller confirm the resulting peer hash is an actual contact
 * before any session state is established; returning false closes the socket immediately —
 * per invariant B.3 there is no fallback path once identity verification fails.
 */
suspend fun performHandshake(
    socket: Socket,
    expectedPeerHash: String?,
    handshakeManager: HandshakeManager,
    onVerified: suspend (peerHash: String) -> Boolean,
): Connection? = withContext(Dispatchers.IO) {
    try {
        val ownHello = handshakeManager.buildHelloPayload()
        writeFrame(socket.outputStream, FrameType.HELLO, ownHello)

        val frame = readFrame(socket.inputStream)
        if (frame.type != FrameType.HELLO) {
            throw HandshakeFormatException("Expected HELLO, got frame type ${frame.type}")
        }
        val verified = handshakeManager.parseAndVerifyHello(frame.payload, expectedPeerHash)

        if (!onVerified(verified.peerHash)) {
            Log.w(TAG, "Rejecting connection from unknown/unauthorized peer")
            socket.close()
            return@withContext null
        }

        handshakeManager.establishOutboundSession(verified.peerHash, verified.bundle)
        buildConnection(verified, socket, handshakeManager)
    } catch (e: PeerIdentityMismatchException) {
        Log.w(TAG, "Peer identity mismatch, closing connection: ${e.message}")
        runCatching { socket.close() }
        null
    } catch (e: HandshakeFormatException) {
        Log.w(TAG, "Malformed handshake, closing connection: ${e.message}")
        runCatching { socket.close() }
        null
    } catch (e: Exception) {
        Log.w(TAG, "Handshake failed: ${e.message}")
        runCatching { socket.close() }
        null
    }
}

private fun buildConnection(verified: cz.twocom.core.crypto.VerifiedHello, socket: Socket, handshakeManager: HandshakeManager): Connection {
    val peerHash = verified.peerHash
    val receiveFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    scope.launch {
        while (isActive) {
            val frame = try {
                readFrame(socket.inputStream)
            } catch (e: Exception) {
                Log.d(TAG, "Connection to $peerHash closed: ${e.message}")
                break
            }
            if (frame.type != FrameType.DATA || frame.payload.isEmpty()) continue
            val ciphertextType = frame.payload[0].toInt() and 0xFF
            val ciphertext = frame.payload.copyOfRange(1, frame.payload.size)
            try {
                val plaintext = handshakeManager.decrypt(peerHash, ciphertextType, ciphertext)
                receiveFlow.emit(plaintext)
            } catch (e: Exception) {
                Log.w(TAG, "Dropping undecryptable DATA frame from $peerHash: ${e.message}")
            }
        }
    }

    return Connection(
        peerId = peerHash,
        peerSigningPublicKey = verified.signingPublicKey,
        peerIdentityPublicKey = verified.identityKey.serialize(),
        send = { plaintext ->
            val (type, ciphertext) = handshakeManager.encrypt(peerHash, plaintext)
            writeFrame(socket.outputStream, FrameType.DATA, byteArrayOf(type.toByte()) + ciphertext)
        },
        receive = receiveFlow,
        close = {
            scope.cancel()
            runCatching { socket.close() }
        },
    )
}
