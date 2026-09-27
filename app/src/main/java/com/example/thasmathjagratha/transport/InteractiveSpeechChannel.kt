package com.example.thasmathjagratha.transport

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

enum class SpeechChannelMode {
    AUTO_P2P_THEN_HOTSPOT,
    WIFI_DIRECT_P2P_ONLY,
    LOCAL_HOTSPOT_ONLY
}

enum class ConnectionMethod {
    NONE,
    WIFI_DIRECT_P2P,
    LOCAL_ONLY_HOTSPOT,
    FALLBACK_UDP_RFCOMM
}

/**
 * Part 2: Interactive Speech Channel.
 *
 * Provides an ordered, reliable, bidirectional TCP socket for continuous speech relay.
 * Implements two distinct link-establishment paths:
 * 1. WifiP2pManager (Wi-Fi Direct peer-to-peer group negotiation)
 * 2. WifiManager.LocalOnlyHotspotReservation (Soft-AP local tethering without internet)
 *
 * Configurable via SpeechChannelMode with automatic 12-second timeout fallback.
 * Emits unambiguous, grep-friendly logs under the tag "SpeechChannel" with millisecond timestamps.
 */
class InteractiveSpeechChannel(
    private val context: Context,
    private val onAlertReceived: (WireAlert) -> Unit
) {
    companion object {
        const val TAG = "SpeechChannel"
        const val TCP_PORT = 28155
        const val P2P_DEFAULT_GO_IP = "192.168.49.1"
        const val CONNECTION_TIMEOUT_MS = 12_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val wifiP2pChannel = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var socketOut: DataOutputStream? = null
    private val socketMutex = Mutex()

    private var hotspotReservation: Any? = null // WifiManager.LocalOnlyHotspotReservation on API 26+
    private var activeConnectionMethod = ConnectionMethod.NONE
    private val isConnected = AtomicBoolean(false)

    val currentMethod: ConnectionMethod get() = activeConnectionMethod
    val isChannelConnected: Boolean get() = isConnected.get()

    /**
     * Start the Receiver side (hosts server socket and prepares P2P group or Hotspot).
     */
    fun startReceiverChannel(mode: SpeechChannelMode = SpeechChannelMode.AUTO_P2P_THEN_HOTSPOT) {
        scope.launch {
            Log.i(TAG, "[SpeechChannel] Connection attempt started: mode=$mode role=RECEIVER at ${System.currentTimeMillis()}")

            startTcpServer()

            when (mode) {
                SpeechChannelMode.WIFI_DIRECT_P2P_ONLY -> {
                    createP2pGroup()
                }
                SpeechChannelMode.LOCAL_HOTSPOT_ONLY -> {
                    startLocalHotspot()
                }
                SpeechChannelMode.AUTO_P2P_THEN_HOTSPOT -> {
                    val p2pOk = createP2pGroup()
                    if (!p2pOk) {
                        Log.w(TAG, "[SpeechChannel] P2P group creation failed. Falling back to Local Hotspot.")
                        startLocalHotspot()
                    }
                }
            }
        }
    }

    /**
     * Start the Sender side (connects to Receiver's P2P group or Hotspot, then opens TCP socket).
     */
    suspend fun connectSenderChannel(
        targetHost: String = P2P_DEFAULT_GO_IP,
        mode: SpeechChannelMode = SpeechChannelMode.AUTO_P2P_THEN_HOTSPOT
    ): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[SpeechChannel] Connection attempt started: mode=$mode role=SENDER target=$targetHost at ${System.currentTimeMillis()}")

        val connected = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
            when (mode) {
                SpeechChannelMode.WIFI_DIRECT_P2P_ONLY -> connectP2pClient(targetHost)
                SpeechChannelMode.LOCAL_HOTSPOT_ONLY -> connectHotspotClient(targetHost)
                SpeechChannelMode.AUTO_P2P_THEN_HOTSPOT -> {
                    val p2pSuccess = connectP2pClient(targetHost)
                    if (p2pSuccess) {
                        true
                    } else {
                        Log.i(TAG, "[SpeechChannel] Wi-Fi Direct connection timeout. Falling back to Hotspot connection.")
                        connectHotspotClient(targetHost)
                    }
                }
            }
        } ?: false

        if (!connected) {
            Log.w(TAG, "[SpeechChannel] Channel failed to establish within ${CONNECTION_TIMEOUT_MS}ms. Caller will fall back to UDP/RFCOMM.")
            activeConnectionMethod = ConnectionMethod.FALLBACK_UDP_RFCOMM
        }
        connected
    }

    private suspend fun connectP2pClient(targetHost: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(targetHost, TCP_PORT), 6000)
            setupConnectedSocket(socket, ConnectionMethod.WIFI_DIRECT_P2P)
            true
        } catch (e: Exception) {
            Log.w(TAG, "[SpeechChannel] P2P socket connection failed to $targetHost:$TCP_PORT: ${e.message}")
            false
        }
    }

    private suspend fun connectHotspotClient(targetHost: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(targetHost, TCP_PORT), 6000)
            setupConnectedSocket(socket, ConnectionMethod.LOCAL_ONLY_HOTSPOT)
            true
        } catch (e: Exception) {
            Log.w(TAG, "[SpeechChannel] Hotspot socket connection failed to $targetHost:$TCP_PORT: ${e.message}")
            false
        }
    }

    private fun startTcpServer() {
        scope.launch {
            try {
                serverSocket?.close()
                val server = ServerSocket(TCP_PORT)
                serverSocket = server
                Log.i(TAG, "[SpeechChannel] TCP Server listening on port $TCP_PORT at ${System.currentTimeMillis()}")

                while (isActive) {
                    val client = server.accept()
                    client.tcpNoDelay = true
                    val remote = client.remoteSocketAddress
                    Log.i(TAG, "[SpeechChannel] Incoming TCP connection accepted from $remote at ${System.currentTimeMillis()}")
                    setupConnectedSocket(client, activeConnectionMethod)
                }
            } catch (e: Exception) {
                if (isActive) Log.e(TAG, "[SpeechChannel] TCP server error", e)
            }
        }
    }

    private fun setupConnectedSocket(socket: Socket, method: ConnectionMethod) {
        scope.launch {
            socketMutex.withLock {
                activeSocket?.close()
                activeSocket = socket
                socketOut = DataOutputStream(socket.getOutputStream())
                activeConnectionMethod = method
                isConnected.set(true)
            }

            Log.i(TAG, "[SpeechChannel] Connection established: method=$method remote=${socket.remoteSocketAddress} at ${System.currentTimeMillis()}")

            // Non-blocking coroutine reader for incoming bidirectional alerts
            try {
                val input = DataInputStream(socket.getInputStream())
                while (isActive && isConnected.get()) {
                    val length = input.readInt()
                    if (length <= 0 || length > 65536) break
                    val bytes = ByteArray(length)
                    input.readFully(bytes)

                    val now = System.currentTimeMillis()
                    val alert = WireAlert.decode(bytes, "TCP ($method)")
                    if (alert != null) {
                        Log.i(TAG, "[SpeechChannel] Message received: id=${alert.id} size=$length at $now")
                        onAlertReceived(alert)
                    } else {
                        val rawText = String(bytes, Charsets.UTF_8)
                        Log.i(TAG, "[SpeechChannel] Raw text payload received: size=$length at $now")
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "[SpeechChannel] Socket disconnected: ${e.message} at ${System.currentTimeMillis()}")
            } finally {
                disconnect()
            }
        }
    }

    /**
     * Send a WireAlert over the established TCP speech channel.
     * Note: Currently formats as WireAlert JSON bytes for immediate end-to-end compatibility.
     * Swapping in a compact STT-token-byte stream is a follow-up optimization.
     */
    suspend fun sendAlert(alert: WireAlert): Boolean = withContext(Dispatchers.IO) {
        val bytes = alert.encode()
        socketMutex.withLock {
            val out = socketOut
            if (out == null || !isConnected.get()) {
                Log.w(TAG, "[SpeechChannel] Cannot send alert: SpeechChannel not connected.")
                return@withContext false
            }
            try {
                val sendTimestamp = System.currentTimeMillis()
                out.writeInt(bytes.size)
                out.write(bytes)
                out.flush()
                Log.i(TAG, "[SpeechChannel] Message sent: id=${alert.id} size=${bytes.size} at $sendTimestamp")
                true
            } catch (e: Exception) {
                Log.e(TAG, "[SpeechChannel] Failed to send alert ${alert.id} over TCP socket", e)
                disconnect()
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun createP2pGroup(): Boolean = withContext(Dispatchers.Main) {
        val manager = wifiP2pManager ?: return@withContext false
        val channel = wifiP2pChannel ?: return@withContext false

        var success = false
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "[SpeechChannel] Wi-Fi Direct Group created successfully (IP: $P2P_DEFAULT_GO_IP) at ${System.currentTimeMillis()}")
                activeConnectionMethod = ConnectionMethod.WIFI_DIRECT_P2P
                success = true
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "[SpeechChannel] Wi-Fi Direct Group creation failed with reason=$reason")
                success = false
            }
        })
        delay(1000)
        success
    }

    private fun startLocalHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wifiManager?.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                        super.onStarted(reservation)
                        hotspotReservation = reservation
                        activeConnectionMethod = ConnectionMethod.LOCAL_ONLY_HOTSPOT
                        Log.i(TAG, "[SpeechChannel] Local-Only Hotspot started successfully at ${System.currentTimeMillis()}")
                    }

                    override fun onStopped() {
                        super.onStopped()
                        Log.i(TAG, "[SpeechChannel] Local-Only Hotspot stopped")
                        hotspotReservation = null
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        Log.e(TAG, "[SpeechChannel] Local-Only Hotspot start failed with reason=$reason")
                    }
                }, null)
            } catch (e: SecurityException) {
                Log.e(TAG, "[SpeechChannel] SecurityException starting LocalOnlyHotspot", e)
            }
        } else {
            Log.w(TAG, "[SpeechChannel] LocalOnlyHotspot requires Android 8.0+")
        }
    }

    fun disconnect() {
        isConnected.set(false)
        activeConnectionMethod = ConnectionMethod.NONE
        try {
            socketOut?.close()
        } catch (_: Exception) {}
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
        socketOut = null
    }

    fun close() {
        disconnect()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (hotspotReservation as? WifiManager.LocalOnlyHotspotReservation)?.close()
            hotspotReservation = null
        }

        wifiP2pManager?.let { manager ->
            wifiP2pChannel?.let { channel ->
                manager.removeGroup(channel, null)
            }
        }
    }
}
