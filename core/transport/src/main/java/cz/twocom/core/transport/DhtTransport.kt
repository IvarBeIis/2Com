package cz.twocom.core.transport

import android.util.Log
import cz.twocom.core.crypto.HandshakeManager
import cz.twocom.core.crypto.IdentityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DhtTransport"

/**
 * Relay/rendezvous pool, not a Kademlia DHT (see ADR-0001 context / 2COM_AUDIT.md #4 and
 * doc/09_Architecture.md's updated description) — connects to whichever seed node from the
 * bootstrap list responds, then relies on the HELLO handshake to actually reach the intended
 * peer. Real DHT-style iterative lookup is out of scope for this pass.
 */
@Singleton
class DhtTransport @Inject constructor(
    private val handshakeManager: HandshakeManager,
    private val identityManager: IdentityManager,
) : Transport {

    override val name = "DHT"
    override val priority = 4

    private val _incoming = MutableSharedFlow<IncomingConnection>(extraBufferCapacity = 8)
    private var seeds: List<DhtPeer> = emptyList()
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var contactLookup: (suspend (peerHash: String) -> Boolean)? = null

    override suspend fun isAvailable(): Boolean = true

    suspend fun initialize(bootstrapUrl: String) {
        seeds = fetchSeeds(bootstrapUrl) ?: DhtBootstrapPeers.HARDCODED_FALLBACK
        Log.d(TAG, "Initialized with ${seeds.size} seed peers")
    }

    /** Starts listening on a local port and announces it (signed) to the bootstrap server. */
    suspend fun startListening(bootstrapUrl: String) {
        val socket = ServerSocket(0)
        serverSocket = socket
        scope.launch {
            while (true) {
                val incoming = try {
                    socket.accept()
                } catch (e: Exception) {
                    Log.d(TAG, "DHT server socket closed: ${e.message}")
                    break
                }
                launch {
                    val allow = contactLookup
                    val conn = performHandshake(incoming, expectedPeerHash = null, handshakeManager = handshakeManager) { hash ->
                        allow?.invoke(hash) ?: false
                    }
                    if (conn != null) _incoming.emit(IncomingConnection(conn.peerId, conn))
                }
            }
        }
        announce(bootstrapUrl, socket.localPort)
    }

    private suspend fun announce(bootstrapUrl: String, port: Int) {
        val identity = identityManager.loadIdentity() ?: return
        val signingPriv = identityManager.loadSigningPrivateKey() ?: return
        val timestamp = System.currentTimeMillis() / 1000
        val message = "${identity.identityHashHex}:$port:$timestamp".toByteArray(Charsets.UTF_8)
        val signer = Ed25519Signer().apply {
            init(true, Ed25519PrivateKeyParameters(signingPriv, 0))
            update(message, 0, message.size)
        }
        val signature = signer.generateSignature().joinToString("") { "%02x".format(it) }

        try {
            withTimeout(5_000) {
                api(bootstrapUrl).announce(
                    AnnounceRequest(
                        node_id = identity.identityHashHex,
                        port = port,
                        timestamp = timestamp,
                        signature = signature,
                        signing_public_key = identity.signingPublicKey.toHex(),
                        identity_public_key = identity.identityPublicKey.toHex(),
                    ),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Announce failed (will rely on hardcoded fallback peers): ${e.message}")
        }
    }

    override suspend fun connect(peerHash: String): Connection? {
        val activePeers = if (seeds.isEmpty()) DhtBootstrapPeers.HARDCODED_FALLBACK else seeds
        for (peer in activePeers) {
            try {
                val socket = withTimeout(10_000) {
                    Socket().also { it.connect(InetSocketAddress(peer.host, peer.port), 5000) }
                }
                if (socket.isConnected) {
                    Log.d(TAG, "Relay-connected via ${peer.region} (${peer.host})")
                    val conn = performHandshake(socket, expectedPeerHash = peerHash, handshakeManager = handshakeManager) { true }
                    if (conn != null) return conn
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect via ${peer.host}: ${e.message}")
            }
        }
        return null
    }

    override fun listen(): Flow<IncomingConnection> = _incoming

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun api(bootstrapUrl: String): BootstrapApi =
        Retrofit.Builder()
            .baseUrl(bootstrapUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BootstrapApi::class.java)

    private suspend fun fetchSeeds(bootstrapUrl: String): List<DhtPeer>? = try {
        withTimeout(5_000) {
            api(bootstrapUrl).getSeeds().seeds.map { DhtPeer(it.host, it.port, it.node_id) }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Bootstrap API failed, using hardcoded fallback: ${e.message}")
        null
    }
}
