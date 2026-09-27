package com.example.thasmathjagratha.transport

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

/**
 * Part 1: Instant Disaster Beacon Scanner.
 *
 * Runs continuously while in Receiver mode.
 * Filters exclusively for ThasmathJagratha disaster beacons matching MANUFACTURER_ID.
 * Implements strict de-duplication on (senderId, sequenceId) within a 60-second window.
 * Emits received alerts directly into the existing WireAlert handler.
 */
class BleBeaconScanner(
    private val context: Context,
    private val onAlertReceived: (WireAlert) -> Unit
) {
    companion object {
        private const val TAG = "BleBeaconScanner"
        private const val DEDUPLICATION_WINDOW_MS = 60_000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    // Deduplication map: key = "$senderId:$sequenceId", value = timestamp
    private val deduplicationMap = ConcurrentHashMap<String, Long>()

    private var activeCallback: ScanCallback? = null
    private var isScanning = false

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @Synchronized
    fun startScanning() {
        if (isScanning) return

        if (!hasScanPermission()) {
            Log.w(TAG, "[BleBeacon] BLUETOOTH_SCAN permission missing. Beacon scanner cannot start.")
            return
        }

        val leScanner = scanner
        if (leScanner == null || bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "[BleBeacon] BluetoothLeScanner unavailable or Bluetooth disabled.")
            return
        }

        val filter = ScanFilter.Builder()
            .setManufacturerData(BleBeaconProtocol.MANUFACTURER_ID, byteArrayOf())
            .build()

        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (bluetoothAdapter?.isLeExtendedAdvertisingSupported == true) {
                settingsBuilder.setLegacy(false)
            }
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { handleScanResult(it) }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "[BleBeacon] BLE Scan failed with errorCode=$errorCode")
            }
        }

        try {
            leScanner.startScan(listOf(filter), settingsBuilder.build(), callback)
            activeCallback = callback
            isScanning = true
            Log.i(TAG, "[BleBeacon] Instant Disaster Beacon Scanner ACTIVE (SCAN_MODE_LOW_LATENCY)")
        } catch (e: SecurityException) {
            Log.e(TAG, "[BleBeacon] SecurityException during startScan", e)
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val record: ScanRecord = result.scanRecord ?: return
        val rawData = record.getManufacturerSpecificData(BleBeaconProtocol.MANUFACTURER_ID) ?: return

        val decoded = BleBeaconProtocol.decode(rawData) ?: return

        val now = System.currentTimeMillis()
        // Purge expired deduplication keys
        deduplicationMap.entries.removeIf { now - it.value > DEDUPLICATION_WINDOW_MS }

        val dedupeKey = "${decoded.senderIdString}:${decoded.sequenceId}"
        if (deduplicationMap.putIfAbsent(dedupeKey, now) != null) {
            // Already received within window
            return
        }

        val wireAlert = decoded.toWireAlert()
        Log.i(TAG, "[BleBeacon] DISASTER BEACON RECEIVED: id=${wireAlert.id} lang=${wireAlert.languageCode} sev=${wireAlert.severity} text=\"${wireAlert.text.take(30)}\" rssi=${result.rssi}")
        onAlertReceived(wireAlert)
    }

    @Synchronized
    fun stopScanning() {
        if (!isScanning) return
        val leScanner = scanner
        activeCallback?.let {
            try {
                leScanner?.stopScan(it)
            } catch (e: SecurityException) {
                Log.w(TAG, "[BleBeacon] SecurityException stopping scanner", e)
            }
        }
        activeCallback = null
        isScanning = false
        Log.i(TAG, "[BleBeacon] Instant Disaster Beacon Scanner STOPPED")
    }
}
