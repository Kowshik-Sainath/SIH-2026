package com.example.thasmathjagratha.transport

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Part 1: Instant Disaster Beacon Advertiser.
 *
 * Uses BLE advertising broadcast without requiring pairing or central coordination.
 * Runtime-detects whether Bluetooth 5.0 LE Extended Advertising is supported:
 * - If supported: Transmits extended beacon (up to 254 bytes) via AdvertisingSet.
 * - If unsupported: Gracefully falls back to legacy advertising (capped at 31 bytes total).
 */
class BleBeaconAdvertiser(
    private val context: Context
) {
    companion object {
        private const val TAG = "BleBeaconAdvertiser"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val advertiser: BluetoothLeAdvertiser? get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val scope = CoroutineScope(Dispatchers.IO)
    private var advertisingJob: Job? = null
    private var activeAdvertiseCallback: AdvertiseCallback? = null
    private var activeAdvertisingSetCallback: AdvertisingSetCallback? = null
    private var activeAdvertisingSet: AdvertisingSet? = null

    private val sequenceCounter = AtomicInteger(1)

    fun isExtendedAdvertisingSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bluetoothAdapter?.isLeExtendedAdvertisingSupported == true
        } else {
            false
        }
    }

    fun hasAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Broadcast an emergency alert as a BLE disaster beacon.
     * @param alert The alert to broadcast
     * @param burstDurationMs Duration to broadcast (default 15,000ms / 15 seconds)
     */
    fun broadcastBeacon(
        alert: WireAlert,
        burstDurationMs: Long = 15_000L,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (!hasAdvertisePermission()) {
            Log.e(TAG, "[BleBeacon] BLUETOOTH_ADVERTISE permission missing. Broadcast aborted.")
            onComplete?.invoke(false)
            return
        }

        val leAdvertiser = advertiser
        if (leAdvertiser == null) {
            Log.e(TAG, "[BleBeacon] BluetoothLeAdvertiser unavailable (Bluetooth disabled or not supported).")
            onComplete?.invoke(false)
            return
        }

        stopAdvertising()

        val seq = sequenceCounter.getAndIncrement()
        val isExtended = isExtendedAdvertisingSupported()

        Log.i(TAG, "[BleBeacon] Starting Disaster Beacon broadcast: id=${alert.id} seq=$seq extended=$isExtended burstMs=$burstDurationMs")

        if (isExtended && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startExtendedAdvertising(leAdvertiser, alert, seq, burstDurationMs, onComplete)
        } else {
            startLegacyAdvertising(leAdvertiser, alert, seq, burstDurationMs, onComplete)
        }
    }

    private fun startLegacyAdvertising(
        leAdvertiser: BluetoothLeAdvertiser,
        alert: WireAlert,
        seq: Int,
        burstDurationMs: Long,
        onComplete: ((Boolean) -> Unit)?
    ) {
        val senderHashShort = (alert.senderId.hashCode() and 0xFFFF).toShort()
        val payload = BleBeaconProtocol.encodeLegacy(
            senderIdShort = senderHashShort,
            seq = (seq and 0xFFFF).toShort(),
            language = alert.languageCode,
            severity = alert.severity,
            text = alert.text
        )

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(burstDurationMs.toInt())
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(BleBeaconProtocol.MANUFACTURER_ID, payload)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i(TAG, "[BleBeacon] Legacy Disaster Beacon active on air (${payload.size} payload bytes)")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "[BleBeacon] Legacy Disaster Beacon failed to advertise: errorCode=$errorCode")
                onComplete?.invoke(false)
            }
        }
        activeAdvertiseCallback = callback

        try {
            leAdvertiser.startAdvertising(settings, data, callback)
            advertisingJob = scope.launch {
                delay(burstDurationMs)
                stopAdvertising()
                onComplete?.invoke(true)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "[BleBeacon] SecurityException during startAdvertising", e)
            onComplete?.invoke(false)
        }
    }

    private fun startExtendedAdvertising(
        leAdvertiser: BluetoothLeAdvertiser,
        alert: WireAlert,
        seq: Int,
        burstDurationMs: Long,
        onComplete: ((Boolean) -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val senderHashInt = alert.senderId.hashCode()
        val payload = BleBeaconProtocol.encodeExtended(
            senderIdInt = senderHashInt,
            seq = seq,
            language = alert.languageCode,
            severity = alert.severity,
            text = alert.text
        )

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(BleBeaconProtocol.MANUFACTURER_ID, payload)
            .build()

        val callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    activeAdvertisingSet = advertisingSet
                    Log.i(TAG, "[BleBeacon] Extended Disaster Beacon active on air (${payload.size} payload bytes)")
                } else {
                    Log.e(TAG, "[BleBeacon] Extended Disaster Beacon failed to start: status=$status")
                    onComplete?.invoke(false)
                }
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                Log.i(TAG, "[BleBeacon] Extended Disaster Beacon burst stopped")
            }
        }
        activeAdvertisingSetCallback = callback

        try {
            leAdvertiser.startAdvertisingSet(parameters, data, null, null, null, callback)
            advertisingJob = scope.launch {
                delay(burstDurationMs)
                stopAdvertising()
                onComplete?.invoke(true)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "[BleBeacon] SecurityException during startAdvertisingSet", e)
            onComplete?.invoke(false)
        }
    }

    fun stopAdvertising() {
        advertisingJob?.cancel()
        advertisingJob = null

        val leAdvertiser = advertiser ?: return

        activeAdvertiseCallback?.let {
            try {
                leAdvertiser.stopAdvertising(it)
            } catch (e: SecurityException) {
                Log.w(TAG, "[BleBeacon] SecurityException stopping legacy advertiser", e)
            }
            activeAdvertiseCallback = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activeAdvertisingSetCallback?.let {
                try {
                    leAdvertiser.stopAdvertisingSet(it)
                } catch (e: SecurityException) {
                    Log.w(TAG, "[BleBeacon] SecurityException stopping extended advertising set", e)
                }
                activeAdvertisingSetCallback = null
                activeAdvertisingSet = null
            }
        }
    }
}
