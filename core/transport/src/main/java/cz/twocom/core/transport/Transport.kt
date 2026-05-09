package cz.twocom.core.transport

import kotlinx.coroutines.flow.Flow

interface Transport {
    val name: String
    val priority: Int
    suspend fun isAvailable(): Boolean
    suspend fun connect(peerHash: String): Connection?
    fun listen(): Flow<IncomingConnection>
}

data class Connection(
    val peerId: String,
    val send: suspend (ByteArray) -> Unit,
    val receive: Flow<ByteArray>,
    val close: suspend () -> Unit,
)

data class IncomingConnection(
    val peerId: String,
    val connection: Connection,
)
