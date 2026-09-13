package cz.twocom.feature.chat

import android.util.Log
import cz.twocom.core.database.dao.ContactDao
import cz.twocom.core.database.dao.MessageDao
import cz.twocom.core.database.entity.MessageEntity
import cz.twocom.core.transport.TransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "IncomingMessageProcessor"

/**
 * Consumes [TransportManager.incomingConnections] — this used to have no subscriber at all
 * (2COM_AUDIT.md #2: the app could send but structurally could not receive). Persists
 * decrypted incoming text messages and marks the sending contact verified once its identity
 * has passed the handshake check in HandshakeManager/SocketHandshake.
 */
@Singleton
class IncomingMessageProcessor @Inject constructor(
    private val transportManager: TransportManager,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            transportManager.incomingConnections.collect { incoming ->
                val conn = incoming.connection
                transportManager.registerIncoming(incoming.peerId, conn)

                val contact = contactDao.findByHash(incoming.peerId)
                if (contact == null) {
                    Log.w(TAG, "Dropping connection from non-contact ${incoming.peerId.take(8)}...")
                    conn.close()
                    return@collect
                }
                contactDao.markVerified(
                    incoming.peerId,
                    conn.peerSigningPublicKey.toHexStringLocal(),
                    conn.peerIdentityPublicKey.toHexStringLocal(),
                    System.currentTimeMillis(),
                )

                scope.launch {
                    conn.receive.collect { plaintext ->
                        val row = contactDao.findByHash(incoming.peerId) ?: return@collect
                        messageDao.insert(
                            MessageEntity(
                                messageUuid = UUID.randomUUID().toString(),
                                contactId = row.id,
                                isSelf = false,
                                text = plaintext.toString(Charsets.UTF_8),
                                status = "DELIVERED",
                                sentAt = System.currentTimeMillis(),
                                deliveredAt = System.currentTimeMillis(),
                                readAt = null,
                                expiresAt = null,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun ByteArray.toHexStringLocal() = joinToString("") { "%02x".format(it) }
