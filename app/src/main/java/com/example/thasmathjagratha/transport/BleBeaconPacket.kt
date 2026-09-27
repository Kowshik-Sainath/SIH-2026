package com.example.thasmathjagratha.transport

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Compact binary protocol for Part 1 (Instant Disaster Beacon).
 *
 * Packet format design:
 * - Legacy Advertising (31-byte limit, ~24 bytes usable payload):
 *   Byte 0:     Opcode (0x01 = Legacy Beacon)
 *   Byte 1..2:  Sender ID Short (16-bit hash / lower bits of sender device ID)
 *   Byte 3..4:  Sequence Number (16-bit unsigned counter, 0..65535)
 *   Byte 5:     Language Code (4 bits) | Severity (4 bits)
 *   Byte 6..N:  Text snippet UTF-8 (up to 18 bytes)
 *
 * - Extended Advertising (up to 254 bytes usable payload):
 *   Byte 0:     Opcode (0x02 = Extended Beacon)
 *   Byte 1..4:  Sender ID Int (32-bit hash of sender device ID)
 *   Byte 5..8:  Sequence Number (32-bit integer)
 *   Byte 9:     Language Code (4 bits) | Severity (4 bits)
 *   Byte 10..11:Text Length (16-bit unsigned short)
 *   Byte 12..N: Full Text UTF-8 (up to 240 bytes)
 */
object BleBeaconProtocol {
    const val MANUFACTURER_ID = 0x0A99 // Custom manufacturer ID for ThasmathJagratha Disaster Relay

    const val OPCODE_LEGACY_BEACON: Byte = 0x01
    const val OPCODE_EXTENDED_BEACON: Byte = 0x02

    private val LANG_TO_ID = mapOf(
        "te" to 0, "telugu" to 0,
        "hi" to 1, "hindi" to 1,
        "en" to 2, "english" to 2,
        "ta" to 3, "tamil" to 3,
        "kn" to 4, "kannada" to 4,
        "ml" to 5, "malayalam" to 5,
        "mr" to 6, "marathi" to 6,
        "bn" to 7, "bengali" to 7,
        "gu" to 8, "gujarati" to 8,
        "or" to 9, "odia" to 9
    )

    private val ID_TO_LANG = listOf(
        "te", "hi", "en", "ta", "kn", "ml", "mr", "bn", "gu", "or"
    )

    private val SEVERITY_TO_ID = mapOf(
        "NORMAL" to 0,
        "IMPORTANT" to 1,
        "HIGH" to 2,
        "CRITICAL" to 3
    )

    private val ID_TO_SEVERITY = listOf(
        "NORMAL", "IMPORTANT", "HIGH", "CRITICAL"
    )

    fun encodeLegacy(
        senderIdShort: Short,
        seq: Short,
        language: String,
        severity: String,
        text: String
    ): ByteArray {
        val langId = LANG_TO_ID[language.lowercase()] ?: 0
        val sevId = SEVERITY_TO_ID[severity.uppercase()] ?: 3
        val meta = ((langId and 0x0F) shl 4 or (sevId and 0x0F)).toByte()

        val textBytes = text.toByteArray(Charsets.UTF_8)
        val textSnippet = if (textBytes.size > 18) textBytes.copyOfRange(0, 18) else textBytes

        val buffer = ByteBuffer.allocate(6 + textSnippet.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(OPCODE_LEGACY_BEACON)
        buffer.putShort(senderIdShort)
        buffer.putShort(seq)
        buffer.put(meta)
        buffer.put(textSnippet)
        return buffer.array()
    }

    fun encodeExtended(
        senderIdInt: Int,
        seq: Int,
        language: String,
        severity: String,
        text: String
    ): ByteArray {
        val langId = LANG_TO_ID[language.lowercase()] ?: 0
        val sevId = SEVERITY_TO_ID[severity.uppercase()] ?: 3
        val meta = ((langId and 0x0F) shl 4 or (sevId and 0x0F)).toByte()

        val textBytes = text.toByteArray(Charsets.UTF_8)
        val maxText = if (textBytes.size > 240) textBytes.copyOfRange(0, 240) else textBytes

        val buffer = ByteBuffer.allocate(12 + maxText.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(OPCODE_EXTENDED_BEACON)
        buffer.putInt(senderIdInt)
        buffer.putInt(seq)
        buffer.put(meta)
        buffer.putShort(maxText.size.toShort())
        buffer.put(maxText)
        return buffer.array()
    }

    fun decode(bytes: ByteArray): DecodedBeacon? = runCatching {
        if (bytes.isEmpty()) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val opcode = buffer.get()

        when (opcode) {
            OPCODE_LEGACY_BEACON -> {
                if (bytes.size < 6) return null
                val senderIdShort = buffer.short
                val seq = buffer.short
                val meta = buffer.get().toInt() and 0xFF
                val langId = (meta shr 4) and 0x0F
                val sevId = meta and 0x0F

                val textBytes = ByteArray(buffer.remaining())
                buffer.get(textBytes)
                val text = String(textBytes, Charsets.UTF_8)

                DecodedBeacon(
                    isExtended = false,
                    senderIdString = "SENDER-%04X".format(senderIdShort),
                    sequenceId = seq.toInt() and 0xFFFF,
                    language = ID_TO_LANG.getOrElse(langId) { "te" },
                    severity = ID_TO_SEVERITY.getOrElse(sevId) { "CRITICAL" },
                    text = text
                )
            }
            OPCODE_EXTENDED_BEACON -> {
                if (bytes.size < 12) return null
                val senderIdInt = buffer.int
                val seq = buffer.int
                val meta = buffer.get().toInt() and 0xFF
                val langId = (meta shr 4) and 0x0F
                val sevId = meta and 0x0F
                val textLen = buffer.short.toInt() and 0xFFFF
                val textBytes = ByteArray(minOf(textLen, buffer.remaining()))
                buffer.get(textBytes)
                val text = String(textBytes, Charsets.UTF_8)

                DecodedBeacon(
                    isExtended = true,
                    senderIdString = "SENDER-%08X".format(senderIdInt),
                    sequenceId = seq,
                    language = ID_TO_LANG.getOrElse(langId) { "te" },
                    severity = ID_TO_SEVERITY.getOrElse(sevId) { "CRITICAL" },
                    text = text
                )
            }
            else -> null
        }
    }.getOrNull()
}

data class DecodedBeacon(
    val isExtended: Boolean,
    val senderIdString: String,
    val sequenceId: Int,
    val language: String,
    val severity: String,
    val text: String
) {
    fun toWireAlert(): WireAlert {
        val transportLabel = if (isExtended) "BLE Extended Beacon" else "BLE Legacy Beacon"
        return WireAlert(
            id = "BEACON-${senderIdString}-${sequenceId}",
            senderId = senderIdString,
            senderName = "Emergency Beacon ($senderIdString)",
            text = text,
            languageCode = language,
            severity = severity,
            sentAt = System.currentTimeMillis(),
            transport = transportLabel
        )
    }
}
