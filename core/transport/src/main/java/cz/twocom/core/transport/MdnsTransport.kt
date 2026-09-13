package cz.twocom.core.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import cz.twocom.core.crypto.HandshakeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val SERVICE_TYPE = "_2com._tcp."
private const val TAG = "MdnsTransport"

@Singleton
class MdnsTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val handshakeManager: HandshakeManager,
) : Transport {

    override val name = "mDNS"
    override val priority = 2

    private val _incoming = MutableSharedFlow<IncomingConnection>(extraBufferCapacity = 8)
    private val nsdManager by lazy { context.getSystemService(NsdManager::class.java) }
    private var serverSocket: ServerSocket? = null
    private var localPort: Int = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Set by TransportManager so accepted inbound sockets can be checked against known contacts. */
    var contactLookup: (suspend (peerHash: String) -> Boolean)? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun connect(peerHash: String): Connection? {
        val resolved = discoverAndResolve(peerHash) ?: return null
        val socket = try {
            Socket().also { it.connect(InetSocketAddress(resolved.host, resolved.port), 3000) }
        } catch (e: Exception) {
            Log.w(TAG, "mDNS socket connect failed: ${e.message}")
            return null
        }
        return performHandshake(socket, expectedPeerHash = peerHash, handshakeManager = handshakeManager) { true }
    }

    private suspend fun discoverAndResolve(peerHash: String): NsdServiceInfo? =
        suspendCancellableCoroutine { cont ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(type: String) {}
                override fun onDiscoveryStopped(type: String) {}
                override fun onStartDiscoveryFailed(type: String, code: Int) { cont.resume(null) }
                override fun onStopDiscoveryFailed(type: String, code: Int) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    // Short prefix is only a discovery filter — the actual identity proof is
                    // the full-hash handshake check performed after connecting (see §1.3 of
                    // the audit: this used to be the *only* check, which was a collision risk).
                    if (info.serviceName.contains(peerHash.take(8))) {
                        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(i: NsdServiceInfo, code: Int) {}
                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                if (cont.isActive) cont.resume(resolved)
                            }
                        })
                    }
                }
                override fun onServiceLost(info: NsdServiceInfo) {}
            }
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            cont.invokeOnCancellation { nsdManager.stopServiceDiscovery(listener) }
        }

    override fun listen(): Flow<IncomingConnection> = _incoming

    fun startAdvertising(peerHash: String) {
        val socket = ServerSocket(0)
        serverSocket = socket
        localPort = socket.localPort

        val info = NsdServiceInfo().apply {
            serviceName = "2com-${peerHash.take(8)}"
            serviceType = SERVICE_TYPE
            port = localPort
        }
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(i: NsdServiceInfo, code: Int) { Log.w(TAG, "NSD register failed $code") }
            override fun onUnregistrationFailed(i: NsdServiceInfo, code: Int) {}
            override fun onServiceRegistered(i: NsdServiceInfo) { Log.d(TAG, "NSD registered: ${i.serviceName}") }
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
        })

        scope.launch {
            while (true) {
                val incomingSocket = try {
                    socket.accept()
                } catch (e: Exception) {
                    Log.d(TAG, "mDNS server socket closed: ${e.message}")
                    break
                }
                launch {
                    val allow = contactLookup
                    val conn = performHandshake(incomingSocket, expectedPeerHash = null, handshakeManager = handshakeManager) { hash ->
                        allow?.invoke(hash) ?: false
                    }
                    if (conn != null) {
                        _incoming.emit(IncomingConnection(conn.peerId, conn))
                    }
                }
            }
        }
    }

    fun stopAdvertising() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}
