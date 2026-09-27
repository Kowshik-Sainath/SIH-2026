package com.example.thasmathjagratha

import com.example.thasmathjagratha.transport.OutboxEntry
import com.example.thasmathjagratha.transport.OutboxStatus
import com.example.thasmathjagratha.transport.OutboxTier
import com.example.thasmathjagratha.transport.WireAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DurableOutboxEntryTest {

    @Test
    fun testOutboxEntryJsonRoundTrip() {
        val now = System.currentTimeMillis()
        val alert = WireAlert(
            id = "alert-101",
            senderId = "device-xyz",
            senderName = "Rescue Officer",
            text = "Cyclone landfall expected in 2 hours",
            languageCode = "en",
            severity = "CRITICAL",
            sentAt = now
        )

        val entry = OutboxEntry(
            id = alert.id,
            alert = alert,
            tier = OutboxTier.SPEECH_CHANNEL,
            status = OutboxStatus.PENDING,
            createdAt = now,
            attempts = 2,
            lastAttemptAt = now + 5000L
        )

        val json = entry.toJson()
        val deserialized = OutboxEntry.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(entry.id, deserialized!!.id)
        assertEquals(entry.tier, deserialized.tier)
        assertEquals(entry.status, deserialized.status)
        assertEquals(entry.createdAt, deserialized.createdAt)
        assertEquals(entry.attempts, deserialized.attempts)
        assertEquals(entry.lastAttemptAt, deserialized.lastAttemptAt)
        assertEquals(alert.text, deserialized.alert.text)
        assertEquals(alert.severity, deserialized.alert.severity)
        assertEquals(alert.languageCode, deserialized.alert.languageCode)
    }

    @Test
    fun testWireAlertRoundTrip() {
        val alert = WireAlert(
            id = "alert-999",
            senderId = "sender-abc",
            senderName = "Volunteer",
            text = "తుఫాను హెచ్చరిక జారీ చేయబడింది",
            languageCode = "te",
            severity = "HIGH",
            sentAt = System.currentTimeMillis()
        )

        val encoded = alert.encode()
        val decoded = WireAlert.decode(encoded, "TestTransport")

        assertNotNull(decoded)
        assertEquals(alert.id, decoded!!.id)
        assertEquals(alert.senderId, decoded.senderId)
        assertEquals(alert.senderName, decoded.senderName)
        assertEquals(alert.text, decoded.text)
        assertEquals(alert.languageCode, decoded.languageCode)
        assertEquals(alert.severity, decoded.severity)
        assertEquals("TestTransport", decoded.transport)
    }
}
