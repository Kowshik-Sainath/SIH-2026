package com.example.thasmathjagratha.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.thasmathjagratha.MainActivity
import com.example.thasmathjagratha.R
import com.example.thasmathjagratha.transport.MultiTierAlertTransport
import com.example.thasmathjagratha.transport.WireAlert
import com.example.thasmathjagratha.tts.engine.TtsEngine
import com.example.thasmathjagratha.tts.engine.TtsMode
import com.example.thasmathjagratha.tts.playback.TtsAudioPlayer
import com.example.thasmathjagratha.tts.telemetry.TtsTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * High-reliability background Foreground Service for Receiver (TTS) Mode.
 * Ensures the receiving phone continuously listens for mesh and Wi-Fi broadcasts
 * on UDP port 28154 and Bluetooth Classic RFCOMM, even when backgrounded in a pocket.
 * Automatically synthesizes and plays incoming alerts with USAGE_ALARM priority.
 */
class AlertReceiverForegroundService : Service() {

    companion object {
        private const val TAG = "AlertReceiverService"
        private const val CHANNEL_ID = "alert_receiver_channel"
        private const val NOTIFICATION_ID = 20261

        const val ACTION_START = "com.example.thasmathjagratha.action.START_RECEIVER"
        const val ACTION_STOP = "com.example.thasmathjagratha.action.STOP_RECEIVER"

        fun start(context: Context) {
            val intent = Intent(context, AlertReceiverForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlertReceiverForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var multiTierTransport: MultiTierAlertTransport? = null
    private var ttsEngine: TtsEngine? = null
    private var ttsPlayer: TtsAudioPlayer? = null
    private val ttsMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ttsEngine = TtsEngine(applicationContext)
        ttsPlayer = TtsAudioPlayer(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()

        if (multiTierTransport == null) {
            multiTierTransport = MultiTierAlertTransport(applicationContext, ::onIncomingAlert).also {
                it.startListening()
            }
            Log.i(TAG, "AlertReceiverForegroundService active with MultiTierAlertTransport (BLE + Wi-Fi Direct/Hotspot + UDP/RFCOMM)")
        }

        return START_STICKY
    }

    private fun startInForeground() {
        val notification = buildNotification("Listening for local emergency broadcasts (Wi-Fi + Bluetooth)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alert Receiver",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors local mesh and Wi-Fi networks for incoming emergency alerts"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TTS Mode: Alert Receiver Active")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun onIncomingAlert(alert: WireAlert) {
        serviceScope.launch {
            Log.i(TAG, "Incoming alert received via ${alert.transport}: [${alert.id}] lang=${alert.languageCode} severity=${alert.severity}")
            ReceiverStateHolder.updateStatus(ReceiverListeningState.RECEIVING, alert.languageCode)
            ReceiverStateHolder.emitReceivedAlert(alert)
            updateNotification("Received alert from ${alert.senderName}: \"${alert.text.take(30)}...\"")

            val langCode = alert.languageCode.lowercase()
            val engine = ttsEngine ?: return@launch
            val isDownloaded = engine.packManager.isPackDownloaded(langCode)

            if (!isDownloaded) {
                Log.w(TAG, "Missing TTS language pack for [$langCode]. Audio playback skipped.")
                ReceiverStateHolder.updateStatus(ReceiverListeningState.IDLE_LISTENING)
                return@launch
            }

            ttsMutex.withLock {
                try {
                    ReceiverStateHolder.updateStatus(ReceiverListeningState.SPEAKING, langCode)
                    val mode = if (alert.severity.equals("CRITICAL", ignoreCase = true)) TtsMode.ALERT else TtsMode.NORMAL
                    engine.init(langCode)
                    val audio = engine.synthesize(alert.text, mode)
                    if (audio != null) {
                        Log.i(TAG, "Playing TTS audio for alert ${alert.id} in [$langCode] (mode=$mode)...")
                        ttsPlayer?.play(audio)
                        TtsTelemetry.logPlayback(alert.id, mode.name)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing TTS for incoming alert", e)
                } finally {
                    ReceiverStateHolder.updateStatus(ReceiverListeningState.IDLE_LISTENING)
                    updateNotification("Listening for local emergency broadcasts (Wi-Fi + Bluetooth)")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Tearing down AlertReceiverForegroundService")
        multiTierTransport?.close()
        multiTierTransport = null
        serviceScope.launch {
            ttsEngine?.destroy()
            ttsEngine = null
        }
        ttsPlayer?.stop()
        ttsPlayer = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
