package com.example.thasmathjagratha

import com.example.thasmathjagratha.transport.BleBeaconProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleBeaconPacketTest {

    @Test
    fun testLegacyEncodingAndDecoding() {
        val senderIdShort: Short = 0x1234
        val seq: Short = 42
        val language = "te"
        val severity = "CRITICAL"
        val text = "వరద" // 12 bytes UTF-8, fits comfortably in <= 18 bytes limit

        val encoded = BleBeaconProtocol.encodeLegacy(
            senderIdShort = senderIdShort,
            seq = seq,
            language = language,
            severity = severity,
            text = text
        )

        // Legacy packet must be compact (<= 24 bytes payload)
        assertTrue("Legacy payload should fit within 24 bytes", encoded.size <= 24)
        assertEquals(BleBeaconProtocol.OPCODE_LEGACY_BEACON, encoded[0])

        val decoded = BleBeaconProtocol.decode(encoded)
        assertNotNull("Decoded beacon must not be null", decoded)
        assertFalse(decoded!!.isExtended)
        assertEquals(42, decoded.sequenceId)
        assertEquals("te", decoded.language)
        assertEquals("CRITICAL", decoded.severity)
        assertEquals(text, decoded.text)

        val wireAlert = decoded.toWireAlert()
        assertEquals("BLE Legacy Beacon", wireAlert.transport)
        assertEquals("te", wireAlert.languageCode)
    }

    @Test
    fun testExtendedEncodingAndDecoding() {
        val senderIdInt = 0x12345678
        val seq = 10005
        val language = "hi"
        val severity = "HIGH"
        val text = "भारी बारिश और बाढ़ की चेतावनी। तुरंत सुरक्षित स्थान पर जाएं।"

        val encoded = BleBeaconProtocol.encodeExtended(
            senderIdInt = senderIdInt,
            seq = seq,
            language = language,
            severity = severity,
            text = text
        )

        assertEquals(BleBeaconProtocol.OPCODE_EXTENDED_BEACON, encoded[0])
        assertTrue("Extended payload should fit within 254 bytes", encoded.size <= 254)

        val decoded = BleBeaconProtocol.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded!!.isExtended)
        assertEquals(10005, decoded.sequenceId)
        assertEquals("hi", decoded.language)
        assertEquals("HIGH", decoded.severity)
        assertEquals(text, decoded.text)

        val wireAlert = decoded.toWireAlert()
        assertEquals("BLE Extended Beacon", wireAlert.transport)
    }

    @Test
    fun testLegacyTextTruncationAt18Bytes() {
        val longText = "This is a very long text exceeding 18 bytes completely"
        val encoded = BleBeaconProtocol.encodeLegacy(
            senderIdShort = 1,
            seq = 1,
            language = "en",
            severity = "NORMAL",
            text = longText
        )

        // 1 byte opcode + 2 bytes senderId + 2 bytes seq + 1 byte meta + max 18 bytes text = 24 bytes
        assertEquals(24, encoded.size)
        val decoded = BleBeaconProtocol.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded!!.text.length <= 18)
    }

    @Test
    fun testCorruptPayloadReturnsNull() {
        val corrupt = byteArrayOf(0x05, 0x01, 0x02) // Invalid opcode
        val decoded = BleBeaconProtocol.decode(corrupt)
        assertNull(decoded)

        val empty = byteArrayOf()
        assertNull(BleBeaconProtocol.decode(empty))
    }
}
