package cz.twocom.core.transport

import android.util.Log
import cz.twocom.core.crypto.IdentityManager
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
    private val identityManager: IdentityManager,
) {
    private val _activeConnections = MutableStateFlow<Map<String, Connection>>(emptyMap())
    val activeConnections = _activeConnections.asStateFlow()

    val incomingConnections: Flow<IncomingConnection> =
        merge(mdnsTransport.listen(), dhtTransport.listen())

    private val transports: List<Transport> = listOf(mdnsTransport, dhtTransport)
        .sortedBy { it.priority }

    /** Set by the caller (ConnectionService) so inbound handshakes can be checked against known contacts. */
    var isKnownContact: (suspend (peerHash: String) -> Boolean)? = null
        set(value) {
            field = value
            mdnsTransport.contactLookup = value
            dhtTransport.contactLookup = value
        }

    suspend fun startListening(bootstrapUrl: String) {
        val identity = identityManager.loadIdentity()
        if (identity == null) {
            Log.w("TransportManager", "No identity yet — not starting listeners")
            return
        }
        mdnsTransport.startAdvertising(identity.identityHashHex)
        dhtTransport.initialize(bootstrapUrl)
        dhtTransport.startListening(bootstrapUrl)
        Log.d("TransportManager", "All transports listening for ${identity.identityHashHex}")
    }

    fun stop() {
        mdnsTransport.stopAdvertising()
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

    fun registerIncoming(peerHash: String, connection: Connection) {
        _activeConnections.value = _activeConnections.value + (peerHash to connection)
    }

    suspend fun disconnect(peerHash: String) {
        _activeConnections.value[peerHash]?.close?.invoke()
        _activeConnections.value = _activeConnections.value - peerHash
    }
}
