package com.example.thasmathjagratha.transport

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class WireAlert(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val languageCode: String,
    val severity: String,
    val sentAt: Long,
    val transport: String = ""
) {
    fun encode(): ByteArray = JSONObject().apply {
        put("v", 1); put("kind", "alert"); put("id", id)
        put("senderId", senderId); put("senderName", senderName)
        put("text", text); put("language", languageCode)
        put("severity", severity); put("sentAt", sentAt)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun decode(data: ByteArray, transport: String): WireAlert? = runCatching {
            val json = JSONObject(String(data, Charsets.UTF_8))
            require(json.getInt("v") == 1 && json.getString("kind") == "alert")
            val alert = WireAlert(
                json.getString("id"), json.getString("senderId"),
                json.getString("senderName"), json.getString("text"),
                json.getString("language"), json.getString("severity"),
                json.getLong("sentAt"), transport
            )
            require(alert.id.length in 1..128 && alert.senderId.length in 1..128)
            require(alert.text.length in 1..2000 && alert.languageCode.length in 2..5)
            require(alert.severity in setOf("NORMAL", "IMPORTANT", "HIGH", "CRITICAL"))
            require(kotlin.math.abs(System.currentTimeMillis() - alert.sentAt) < 24L * 60 * 60 * 1000)
            alert
        }.getOrNull()
    }
}

/** Receives app alerts on the same Wi-Fi LAN and over paired Bluetooth Classic RFCOMM. */
class LocalAlertTransport(
    private val context: Context,
    private val onAlert: ((WireAlert) -> Unit)? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ownId = context.getSharedPreferences("transport", Context.MODE_PRIVATE).let { prefs ->
        prefs.getString("install_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("install_id", it).apply()
        }
    }
    private val seen = ConcurrentHashMap<String, Long>()
    private val outboxFile = File(context.filesDir, "transport/alert-outbox.json")
    private val outboxMutex = Mutex()
    private val pending = LinkedHashMap<String, WireAlert>()
    private var outboxLoaded = false
    private var udpSocket: DatagramSocket? = null
    private var btServer: BluetoothServerSocket? = null
    private var bluetoothStarting = false
    private var started = false

    val deviceId: String get() = ownId

    @Synchronized fun startListening() {
        if (started || onAlert == null) return
        started = true
        Log.i(TAG, "Starting local transport receive listeners (Wi-Fi + Bluetooth)")
        startWifi()
        startBluetooth()
        scope.launch { retryPendingAlerts() }
    }

    fun start() = startListening()

    private fun startWifi() {
        scope.launch {
            try {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(PORT))
                }
                udpSocket = socket
                val buffer = ByteArray(MAX_PACKET)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    receive(packet.data.copyOfRange(0, packet.length), "Wi-Fi")
                }
            } catch (error: Exception) {
                if (scope.isActive) Log.e(TAG, "Wi-Fi alert listener stopped", error)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Synchronized private fun startBluetooth() {
        if (!hasBluetoothPermission()) return
        if (bluetoothStarting || btServer != null) return
        bluetoothStarting = true
        scope.launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    bluetoothStarting = false
                    return@launch
                }
                val server = adapter.listenUsingRfcommWithServiceRecord("Thasmath Alerts", BT_UUID)
                btServer = server
                bluetoothStarting = false
                while (isActive) {
                    val socket = server.accept()
                    launch { socket.use { receiveBluetooth(it) } }
                }
            } catch (error: Exception) {
                bluetoothStarting = false
                if (scope.isActive) Log.e(TAG, "Bluetooth alert listener stopped", error)
            }
        }
    }

    fun startBluetoothIfPermitted() = startBluetooth()

    private fun receiveBluetooth(socket: BluetoothSocket) {
        val input = DataInputStream(socket.inputStream)
        val length = input.readInt()
        if (length !in 1..MAX_PACKET) return
        val bytes = ByteArray(length)
        input.readFully(bytes)
        receive(bytes, "Bluetooth")
    }

    private fun receive(bytes: ByteArray, via: String) {
        val alert = WireAlert.decode(bytes, via) ?: return
        if (alert.senderId == ownId) return
        val now = System.currentTimeMillis()
        seen.entries.removeIf { now - it.value > 24L * 60 * 60 * 1000 }
        if (seen.putIfAbsent(alert.id, now) != null) return
        Log.i(TAG, "Received ${alert.id} via $via")
        onAlert?.invoke(alert)
    }

    /**
     * Queues first, then tries immediately. If there is no usable local link, the alert remains
     * on disk and is retried after restart and every few seconds while this process is alive.
     */
    suspend fun send(alert: WireAlert): Int {
        val bytes = alert.encode()
        require(bytes.size <= MAX_PACKET) { "Alert is too long for local transport" }
        outboxMutex.withLock {
            loadOutboxLocked()
            pending[alert.id] = alert
            persistOutboxLocked()
        }
        return flushPendingAlerts()
    }

    suspend fun pendingCount(): Int = outboxMutex.withLock {
        loadOutboxLocked()
        pending.size
    }

    private suspend fun retryPendingAlerts() {
        while (scope.isActive) {
            runCatching { flushPendingAlerts() }
                .onFailure { Log.w(TAG, "Alert outbox retry failed", it) }
            delay(RETRY_INTERVAL_MS)
        }
    }

    private suspend fun flushPendingAlerts(): Int = outboxMutex.withLock {
        loadOutboxLocked()
        if (pending.isEmpty()) return@withLock 0
        val snapshot = pending.values.toList()
        var attempts = 0
        snapshot.forEach { alert ->
            val delivered = transmit(alert.encode())
            attempts += delivered
            if (delivered > 0) {
                pending.remove(alert.id)
                Log.i(TAG, "Queued alert ${alert.id} sent ($delivered local link send(s))")
            }
        }
        persistOutboxLocked()
        attempts
    }

    private suspend fun transmit(bytes: ByteArray): Int = kotlinx.coroutines.withContext(Dispatchers.IO) {
            var attempts = 0
            val broadcastAddresses = mutableSetOf<java.net.InetAddress>()
            runCatching {
                val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivity.activeNetwork
                val wifi = network != null && connectivity.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (wifi) {
                    val name = connectivity.getLinkProperties(network)?.interfaceName
                    NetworkInterface.getByName(name)?.interfaceAddresses
                        ?.filter { it.address is Inet4Address }
                        ?.mapNotNullTo(broadcastAddresses) { it.broadcast }
                }
            }
            if (broadcastAddresses.isNotEmpty()) {
                runCatching {
                    DatagramSocket().use { socket ->
                        socket.broadcast = true
                        broadcastAddresses.forEach { address ->
                            runCatching { socket.send(DatagramPacket(bytes, bytes.size, address, PORT)) }
                                .onSuccess { attempts++ }
                                .onFailure { Log.e(TAG, "Wi-Fi broadcast failed", it) }
                        }
                    }
                }.onFailure { Log.e(TAG, "Wi-Fi socket failed", it) }
            }
            if (hasBluetoothPermission()) {
                @Suppress("DEPRECATION") val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter?.isEnabled == true) {
                    @Suppress("MissingPermission")
                    adapter.bondedDevices.forEach { device ->
                        runCatching {
                            device.createRfcommSocketToServiceRecord(BT_UUID).use { socket ->
                                socket.connect()
                                DataOutputStream(socket.outputStream).apply {
                                    writeInt(bytes.size); write(bytes); flush()
                                }
                                attempts++
                            }
                        }.onFailure { Log.w(TAG, "Bluetooth peer did not accept alert", it) }
                    }
                }
            }
            attempts
    }

    private fun loadOutboxLocked() {
        if (outboxLoaded) return
        outboxLoaded = true
        if (!outboxFile.isFile) return
        runCatching {
            val array = JSONArray(outboxFile.readText())
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                WireAlert.decode(json.toString().toByteArray(Charsets.UTF_8), "")?.let {
                    pending[it.id] = it.copy(transport = "")
                }
            }
        }.onFailure { Log.w(TAG, "Discarding corrupt alert outbox", it) }
    }

    private fun persistOutboxLocked() {
        outboxFile.parentFile?.mkdirs()
        val temporary = File(outboxFile.parentFile, "${outboxFile.name}.partial")
        val array = JSONArray()
        pending.values.forEach { alert ->
            array.put(JSONObject(String(alert.encode(), Charsets.UTF_8)))
        }
        temporary.writeText(array.toString())
        check(temporary.renameTo(outboxFile)) { "Unable to persist alert outbox" }
    }

    private fun hasBluetoothPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun close() {
        udpSocket?.close()
        btServer?.close()
        btServer = null
        bluetoothStarting = false
        scope.cancel()
    }

    companion object {
        private const val TAG = "LocalAlertTransport"
        private const val PORT = 28154
        private const val MAX_PACKET = 8192
        private const val RETRY_INTERVAL_MS = 5_000L
        private val BT_UUID = UUID.fromString("01f23f8b-37ec-4fa2-8bd7-c93034362976")
    }
}
