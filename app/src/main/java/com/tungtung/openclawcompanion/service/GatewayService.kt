package com.tungtung.openclawcompanion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tungtung.openclawcompanion.MainActivity
import com.tungtung.openclawcompanion.R
import com.tungtung.openclawcompanion.data.ConnectionState
import com.tungtung.openclawcompanion.data.FilterConfigStore
import com.tungtung.openclawcompanion.data.ForwardedEvent
import com.tungtung.openclawcompanion.data.GatewayClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob

class GatewayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var stateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(ConnectionState.DISCONNECTED))
        stateJob = serviceScope.launch {
            GatewayClient.state.collectLatest { state ->
                NotificationManagerCompat.from(this@GatewayService)
                    .notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SEND_EVENT -> handleSendEvent(intent)
            ACTION_RESTART -> restartConnection()
            ACTION_START -> ensureConnectionLoop()
            else -> ensureConnectionLoop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionJob?.cancel()
        stateJob?.cancel()
        serviceScope.cancel()
        GatewayClient.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureConnectionLoop() {
        if (connectionJob?.isActive == true) return
        connectionJob = serviceScope.launch {
            val backoffs = longArrayOf(5_000L, 10_000L, 30_000L, 60_000L)
            var backoffIndex = 0
            while (isActive) {
                val prefs = FilterConfigStore.loadGatewayPrefs()
                GatewayClient.connect(prefs.url, prefs.token)
                val firstState = GatewayClient.state.drop(1).first { it != ConnectionState.CONNECTING }
                if (firstState == ConnectionState.CONNECTED) {
                    backoffIndex = 0
                    GatewayClient.state.first {
                        it == ConnectionState.ERROR || it == ConnectionState.DISCONNECTED
                    }
                }
                val wait = backoffs[backoffIndex]
                backoffIndex = (backoffIndex + 1).coerceAtMost(backoffs.lastIndex)
                delay(wait)
            }
        }
    }

    private fun restartConnection() {
        connectionJob?.cancel()
        connectionJob = null
        ensureConnectionLoop()
    }

    private fun handleSendEvent(intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        if (type.isNotBlank() && source.isNotBlank() && text.isNotBlank()) {
            val event = ForwardedEvent(
                timestamp = System.currentTimeMillis(),
                type = type,
                source = source,
                text = text
            )
            broadcastEvent(event)
            GatewayClient.sendEvent(type, source, text)
        }
    }

    private fun broadcastEvent(event: ForwardedEvent) {
        val intent = Intent(ACTION_EVENT_BROADCAST).apply {
            putExtra(EXTRA_EVENT_TIMESTAMP, event.timestamp)
            putExtra(EXTRA_EVENT_TYPE, event.type)
            putExtra(EXTRA_EVENT_SOURCE, event.source)
            putExtra(EXTRA_EVENT_TEXT, event.text)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(state: ConnectionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val content = when (state) {
            ConnectionState.CONNECTED -> getString(R.string.notification_connected)
            ConnectionState.CONNECTING -> getString(R.string.notification_connecting)
            ConnectionState.ERROR -> getString(R.string.notification_error)
            ConnectionState.DISCONNECTED -> getString(R.string.notification_disconnected)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_companion)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "openclaw_gateway"
        private const val NOTIFICATION_ID = 42

        private const val ACTION_START = "com.tungtung.openclawcompanion.action.START"
        private const val ACTION_STOP = "com.tungtung.openclawcompanion.action.STOP"
        private const val ACTION_RESTART = "com.tungtung.openclawcompanion.action.RESTART"
        private const val ACTION_SEND_EVENT = "com.tungtung.openclawcompanion.action.SEND_EVENT"

        private const val EXTRA_TYPE = "extra_type"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_TEXT = "extra_text"
        const val EXTRA_EVENT_TIMESTAMP = "extra_event_timestamp"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EXTRA_EVENT_SOURCE = "extra_event_source"
        const val EXTRA_EVENT_TEXT = "extra_event_text"
        const val ACTION_EVENT_BROADCAST = "com.tungtung.openclawcompanion.EVENT_BROADCAST"

        fun start(context: Context) {
            val intent = Intent(context, GatewayService::class.java).apply { action = ACTION_START }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GatewayService::class.java))
        }

        fun restart(context: Context) {
            val intent = Intent(context, GatewayService::class.java).apply { action = ACTION_RESTART }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendEvent(context: Context, type: String, source: String, text: String) {
            val intent = Intent(context, GatewayService::class.java).apply {
                action = ACTION_SEND_EVENT
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_SOURCE, source)
                putExtra(EXTRA_TEXT, text)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
