package cz.twocom.core.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
) : Transport {

    override val name = "mDNS"
    override val priority = 2

    private val _incoming = MutableSharedFlow<IncomingConnection>()
    private val nsdManager by lazy { context.getSystemService(NsdManager::class.java) }
    private var serverSocket: ServerSocket? = null
    private var localPort: Int = 0

    override suspend fun isAvailable(): Boolean = true

    override suspend fun connect(peerHash: String): Connection? {
        return suspendCancellableCoroutine { cont ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(type: String) {}
                override fun onDiscoveryStopped(type: String) {}
                override fun onStartDiscoveryFailed(type: String, code: Int) { cont.resume(null) }
                override fun onStopDiscoveryFailed(type: String, code: Int) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    if (info.serviceName.contains(peerHash.take(8))) {
                        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(i: NsdServiceInfo, code: Int) {}
                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                try {
                                    val socket = Socket()
                                    socket.connect(InetSocketAddress(resolved.host, resolved.port), 3000)
                                    val conn = buildConnection(peerHash, socket)
                                    cont.resume(conn)
                                } catch (e: Exception) {
                                    Log.w(TAG, "mDNS connect failed: ${e.message}")
                                    cont.resume(null)
                                }
                            }
                        })
                    }
                }
                override fun onServiceLost(info: NsdServiceInfo) {}
            }
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            cont.invokeOnCancellation { nsdManager.stopServiceDiscovery(listener) }
        }
    }

    override fun listen(): Flow<IncomingConnection> = _incoming

    fun startAdvertising(peerHash: String) {
        serverSocket = ServerSocket(0).also { localPort = it.localPort }
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
    }

    private fun buildConnection(peerId: String, socket: Socket): Connection {
        val receiveFlow = MutableSharedFlow<ByteArray>()
        return Connection(
            peerId = peerId,
            send = { data -> socket.outputStream.write(data); socket.outputStream.flush() },
            receive = receiveFlow,
            close = { socket.close() },
        )
    }
}
