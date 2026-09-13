package cz.twocom.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cz.twocom.BuildConfig
import cz.twocom.core.database.dao.ContactDao
import cz.twocom.core.transport.TransportManager
import cz.twocom.feature.chat.IncomingMessageProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionService : Service() {

    @Inject lateinit var transportManager: TransportManager
    @Inject lateinit var incomingMessageProcessor: IncomingMessageProcessor
    @Inject lateinit var contactDao: ContactDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val channelId = "2com_connection"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        transportManager.isKnownContact = { peerHash -> contactDao.findByHash(peerHash) != null }
        incomingMessageProcessor.start(scope)
        scope.launch { transportManager.startListening(BuildConfig.DHT_BOOTSTRAP_URL) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        transportManager.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "2Com Connection",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("2Com")
            .setContentText("P2P connection active")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()
}
