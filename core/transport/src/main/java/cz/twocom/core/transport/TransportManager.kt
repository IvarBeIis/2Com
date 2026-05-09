package cz.twocom.core.transport

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportManager @Inject constructor(
    private val mdnsTransport: MdnsTransport,
    private val dhtTransport: DhtTransport,
) {
    private val _activeConnections = MutableStateFlow<Map<String, Connection>>(emptyMap())
    val activeConnections = _activeConnections.asStateFlow()

    val incomingConnections: Flow<IncomingConnection> =
        merge(mdnsTransport.listen(), dhtTransport.listen())

    private val transports: List<Transport> = listOf(mdnsTransport, dhtTransport)
        .sortedBy { it.priority }

    suspend fun startListening() {
        Log.d("TransportManager", "Starting all transports")
    }

    fun stop() {
        Log.d("TransportManager", "Stopping all transports")
    }

    suspend fun connect(peerHash: String): Connection? {
        for (transport in transports) {
            if (!transport.isAvailable()) continue
            try {
                val conn = transport.connect(peerHash)
                if (conn != null) {
                    _activeConnections.value = _activeConnections.value + (peerHash to conn)
                    Log.d("TransportManager", "Connected to $peerHash via ${transport.name}")
                    return conn
                }
            } catch (e: Exception) {
                Log.w("TransportManager", "Transport ${transport.name} failed: ${e.message}")
            }
        }
        return null
    }

    fun getConnection(peerHash: String): Connection? = _activeConnections.value[peerHash]

    suspend fun disconnect(peerHash: String) {
        _activeConnections.value[peerHash]?.close?.invoke()
        _activeConnections.value = _activeConnections.value - peerHash
    }
}
