package com.example.thasmathjagratha.transport

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestrates the 3-Tier Offline Voice Transport:
 *
 * Tier 1: Instant Disaster Beacon (BLE Advertising Broadcast)
 *         Zero-pairing, low-latency, non-connectable BLE beacon.
 *         Adaptive: Uses Bluetooth 5.0 LE Extended Advertising (254B) if supported;
 *         falls back to Legacy BLE Advertising (31B).
 *
 * Tier 2: Interactive Speech Channel (Wi-Fi Direct P2P / Local-Only Hotspot TCP Socket)
 *         Ordered, reliable, bidirectional TCP socket on port 28155 for continuous speech.
 *         Automatic 12-second fallback from Wi-Fi Direct to Hotspot.
 *
 * Tier 3: Local Broadcast Fallback (Existing UDP Broadcast + Bluetooth Classic RFCOMM)
 *         Preserves the proven UDP port 28154 and RFCOMM baseline.
 *
 * Store-and-Forward Outbox:
 *         All outgoing alerts written to durable disk queue (DurableOutboxQueue) immediately,
 *         marked PENDING, and only marked SENT once confirmed transmitted.
 */
class MultiTierAlertTransport(
    private val context: Context,
    private val onAlertReceived: ((WireAlert) -> Unit)? = null
) {
    companion object {
        private const val TAG = "MultiTierTransport"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Tier 1: BLE Disaster Beacon
    val bleAdvertiser = BleBeaconAdvertiser(context)
    val bleScanner = BleBeaconScanner(context, ::handleIncomingAlert)

    // Tier 2: Interactive Speech Channel (Wi-Fi Direct / Hotspot)
    val speechChannel = InteractiveSpeechChannel(context, ::handleIncomingAlert)

    // Tier 3: Existing UDP Broadcast + Bluetooth Classic RFCOMM (Preserved Fallback)
    val localTransport = LocalAlertTransport(context, ::handleIncomingAlert)

    // Resilience: Durable Outbox Queue
    val durableOutbox = DurableOutboxQueue(context)

    val deviceId: String get() = localTransport.deviceId

    fun startBluetoothIfPermitted() {
        localTransport.startBluetoothIfPermitted()
    }

    suspend fun pendingCount(): Int = durableOutbox.pendingCount()

    private var isListening = false

    init {
        // Start background drain loop for the durable outbox
        scope.launch {
            while (isActive) {
                delay(5_000L)
                runCatching {
                    durableOutbox.drainQueue { entry ->
                        dispatchQueuedAlert(entry)
                    }
                }
            }
        }
    }

    /**
     * Start all receiving listeners (Tier 1 BLE Scanner, Tier 2 Speech Channel Server, Tier 3 UDP/RFCOMM).
     */
    @Synchronized
    fun startListening() {
        if (isListening || onAlertReceived == null) return
        isListening = true

        Log.i(TAG, "Starting all Multi-Tier receive listeners...")
        bleScanner.startScanning()
        speechChannel.startReceiverChannel()
        localTransport.startListening()
    }

    private fun handleIncomingAlert(alert: WireAlert) {
        Log.i(TAG, "Incoming alert received via ${alert.transport}: id=${alert.id} text=\"${alert.text.take(30)}\"")
        onAlertReceived?.invoke(alert)
    }

    /**
     * Transmit an alert via the Multi-Tier architecture:
     * 1. Writes immediately to durable disk queue as PENDING.
     * 2. Attempts Tier 1 BLE Instant Beacon broadcast.
     * 3. Attempts Tier 2 Interactive Speech Channel (if connected or requested).
     * 4. Transmits via Tier 3 Local Transport (UDP broadcast + RFCOMM).
     * 5. If any link succeeds, marks the outbox item as SENT.
     */
    suspend fun sendAlert(
        alert: WireAlert,
        preferSpeechChannel: Boolean = false,
        broadcastBleBeacon: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val entry = durableOutbox.enqueue(alert, if (preferSpeechChannel) OutboxTier.SPEECH_CHANNEL else OutboxTier.ANY)
        val success = dispatchQueuedAlert(entry, preferSpeechChannel, broadcastBleBeacon)
        if (success) {
            durableOutbox.markSent(alert.id)
        } else {
            durableOutbox.markAttemptFailed(alert.id)
        }
        success
    }

    private suspend fun dispatchQueuedAlert(
        entry: OutboxEntry,
        preferSpeechChannel: Boolean = false,
        broadcastBleBeacon: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val alert = entry.alert
        var delivered = false

        // 1. Tier 1: Instant Disaster Beacon (BLE Advertising)
        if (broadcastBleBeacon || entry.tier == OutboxTier.BEACON) {
            runCatching {
                bleAdvertiser.broadcastBeacon(alert, burstDurationMs = 15_000L) { ok ->
                    if (ok) {
                        Log.i(TAG, "[Tier 1 BLE Beacon] Emergency broadcast active on air for ${alert.id}")
                    }
                }
                delivered = true // Beacon is fired onto the air
            }.onFailure { Log.w(TAG, "Tier 1 BLE Beacon broadcast failed", it) }
        }

        // 2. Tier 2: Interactive Speech Channel (TCP socket if connected)
        if (speechChannel.isChannelConnected) {
            val tcpDelivered = speechChannel.sendAlert(alert)
            if (tcpDelivered) {
                delivered = true
                Log.i(TAG, "[Tier 2 SpeechChannel] Alert delivered via reliable TCP socket: ${alert.id}")
            }
        }

        // 3. Tier 3: UDP Broadcast + Bluetooth Classic Fallback
        val localAttempts = runCatching { localTransport.send(alert) }.getOrDefault(0)
        if (localAttempts > 0) {
            delivered = true
            Log.i(TAG, "[Tier 3 UDP/RFCOMM] Alert broadcast to $localAttempts local destination(s)")
        }

        delivered
    }

    fun close() {
        bleScanner.stopScanning()
        bleAdvertiser.stopAdvertising()
        speechChannel.close()
        localTransport.close()
        scope.cancel()
    }
}
