package cz.twocom.core.transport

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DhtTransport @Inject constructor() : Transport {

    override val name = "DHT"
    override val priority = 4

    private val _incoming = MutableSharedFlow<IncomingConnection>()
    private var seeds: List<DhtPeer> = emptyList()

    override suspend fun isAvailable(): Boolean = true

    suspend fun initialize(bootstrapUrl: String) {
        seeds = fetchSeeds(bootstrapUrl) ?: DhtBootstrapPeers.HARDCODED_FALLBACK
        Log.d("DhtTransport", "Initialized with ${seeds.size} seed peers")
    }

    override suspend fun connect(peerHash: String): Connection? {
        val activePeers = if (seeds.isEmpty()) DhtBootstrapPeers.HARDCODED_FALLBACK else seeds
        for (peer in activePeers) {
            try {
                val socket = withTimeout(10_000) {
                    Socket().also { it.connect(InetSocketAddress(peer.host, peer.port), 5000) }
                }
                if (socket.isConnected) {
                    Log.d("DhtTransport", "Connected via ${peer.region} (${peer.host})")
                    return buildConnection(peerHash, socket)
                }
            } catch (e: Exception) {
                Log.w("DhtTransport", "Failed to connect via ${peer.host}: ${e.message}")
            }
        }
        return null
    }

    override fun listen(): Flow<IncomingConnection> = _incoming

    private fun buildConnection(peerId: String, socket: Socket): Connection {
        val receiveFlow = MutableSharedFlow<ByteArray>()
        return Connection(
            peerId = peerId,
            send = { data ->
                socket.outputStream.write(data)
                socket.outputStream.flush()
            },
            receive = receiveFlow,
            close = { socket.close() },
        )
    }

    private suspend fun fetchSeeds(bootstrapUrl: String): List<DhtPeer>? = try {
        withTimeout(5_000) {
            val api = Retrofit.Builder()
                .baseUrl(bootstrapUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BootstrapApi::class.java)
            api.getSeeds().seeds.map { DhtPeer(it.host, it.port, it.node_id) }
        }
    } catch (e: Exception) {
        Log.w("DhtTransport", "Bootstrap API failed, using hardcoded fallback: ${e.message}")
        null
    }
}
