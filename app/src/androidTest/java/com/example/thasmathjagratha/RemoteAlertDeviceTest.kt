package com.example.thasmathjagratha

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.thasmathjagratha.transport.WireAlert
import com.example.thasmathjagratha.transport.LocalAlertTransport
import com.example.thasmathjagratha.tts.manager.TtsLanguagePackManager
import com.example.thasmathjagratha.tts.telemetry.TtsTelemetry
import com.example.thasmathjagratha.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RemoteAlertDeviceTest {
    @Test fun wifiSenderBroadcastsOnActiveNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val transport = LocalAlertTransport(context) {}
        val sent = transport.send(WireAlert("SEND-${UUID.randomUUID()}", transport.deviceId,
            "Test sender", "సహాయం కావాలి", "te", "CRITICAL", System.currentTimeMillis()))
        assertTrue("No Wi-Fi broadcast was attempted", sent > 0)
        transport.close()
    }

    @Test fun wifiAlertReachesReceiverAndStartsOfflineTts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TtsLanguagePackManager(context).download("te")
        val viewModel = MainViewModel()
        viewModel.startLocalAlertReceiver(context)
        delay(500) // Listener binds asynchronously on the IO dispatcher.
        val id = "DEVICE-${UUID.randomUUID()}"
        val packet = WireAlert(id, "other-device", "Test sender", "సహాయం కావాలి",
            "te", "CRITICAL", System.currentTimeMillis())
        val bytes = packet.encode()
        DatagramSocket().use { socket ->
            socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getLoopbackAddress(), 28154))
        }
        val received = withTimeout(5_000) {
            viewModel.activeCriticalAlert.first { it?.alertId == id }
        }
        assertEquals("సహాయం కావాలి", received?.translatedMessage)
        assertEquals(false, received?.verified)
        assertEquals("Wi-Fi", viewModel.receivedAlerts.value.first { it.alertId == id }.receivedVia)
        withTimeout(180_000) {
            TtsTelemetry.lines.first { lines -> lines.any { it == "TTS playback complete: $id (ALERT)" } }
        }
        assertTrue(TtsTelemetry.lines.value.any { it.startsWith("TTS te:") })
        viewModel.acknowledgeAlert(id)
    }
}
