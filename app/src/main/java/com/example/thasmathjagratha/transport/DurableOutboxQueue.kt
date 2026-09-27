package com.example.thasmathjagratha.transport

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class OutboxTier {
    BEACON,
    SPEECH_CHANNEL,
    ANY
}

enum class OutboxStatus {
    PENDING,
    SENT,
    FAILED
}

data class OutboxEntry(
    val id: String,
    val alert: WireAlert,
    val tier: OutboxTier = OutboxTier.ANY,
    var status: OutboxStatus = OutboxStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    var attempts: Int = 0,
    var lastAttemptAt: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("alert", JSONObject(String(alert.encode(), Charsets.UTF_8)))
        put("tier", tier.name)
        put("status", status.name)
        put("createdAt", createdAt)
        put("attempts", attempts)
        put("lastAttemptAt", lastAttemptAt)
    }

    companion object {
        fun fromJson(json: JSONObject): OutboxEntry? = runCatching {
            val alertJson = json.getJSONObject("alert")
            val alert = WireAlert.decode(alertJson.toString().toByteArray(Charsets.UTF_8), "") ?: return null
            OutboxEntry(
                id = json.getString("id"),
                alert = alert,
                tier = OutboxTier.valueOf(json.optString("tier", "ANY")),
                status = OutboxStatus.valueOf(json.optString("status", "PENDING")),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                attempts = json.optInt("attempts", 0),
                lastAttemptAt = json.optLong("lastAttemptAt", 0L)
            )
        }.getOrNull()
    }
}

/**
 * Part 3: Resilience - Durable Store-and-Forward Outbox Queue.
 *
 * Guarantees that outgoing emergency messages are written to durable local storage
 * (file-backed JSON queue) the moment they are created, marked PENDING, and only
 * marked SENT once transmitted via Beacon, Speech Channel, or Fallback UDP/RFCOMM.
 *
 * Automatically loaded from disk on app startup, ensuring messages survive app kills/reboots.
 * Periodically drains oldest-first with exponential backoff on any link availability.
 */
class DurableOutboxQueue(
    private val context: Context
) {
    companion object {
        private const val TAG = "DurableOutbox"
        private const val OUTBOX_FILENAME = "durable-outbox.json"
        private const val BASE_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 60_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(context.filesDir, "transport/$OUTBOX_FILENAME")
    private val mutex = Mutex()
    private val queue = LinkedHashMap<String, OutboxEntry>() // Ordered by insertion (oldest first)
    private var isLoaded = false

    init {
        // Load existing pending messages from disk immediately on initialization
        scope.launch {
            mutex.withLock {
                loadFromDiskLocked()
                Log.i(TAG, "[Outbox] Initialized on startup: ${queue.size} pending message(s) loaded from disk")
            }
        }
    }

    /**
     * Write alert to durable disk queue immediately.
     */
    suspend fun enqueue(alert: WireAlert, tier: OutboxTier = OutboxTier.ANY): OutboxEntry = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadFromDiskLocked()
            val entry = OutboxEntry(
                id = alert.id,
                alert = alert,
                tier = tier,
                status = OutboxStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            queue[entry.id] = entry
            saveToDiskLocked()
            Log.i(TAG, "[Outbox] Enqueued alert to durable disk: id=${entry.id} tier=$tier queueSize=${queue.size}")
            entry
        }
    }

    /**
     * Mark an alert as successfully delivered and remove from pending queue.
     */
    suspend fun markSent(alertId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadFromDiskLocked()
            val entry = queue.remove(alertId)
            if (entry != null) {
                entry.status = OutboxStatus.SENT
                saveToDiskLocked()
                Log.i(TAG, "[Outbox] Alert marked SENT and cleared from durable queue: id=$alertId")
            }
        }
    }

    suspend fun markAttemptFailed(alertId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadFromDiskLocked()
            queue[alertId]?.let { entry ->
                entry.attempts++
                entry.lastAttemptAt = System.currentTimeMillis()
                saveToDiskLocked()
                Log.w(TAG, "[Outbox] Transmission attempt failed for id=$alertId attempts=${entry.attempts}")
            }
        }
    }

    suspend fun pendingEntries(): List<OutboxEntry> = mutex.withLock {
        loadFromDiskLocked()
        queue.values.toList() // preserves oldest-first ordering
    }

    suspend fun pendingCount(): Int = mutex.withLock {
        loadFromDiskLocked()
        queue.size
    }

    /**
     * Drain the queue oldest-first using the provided multi-tier transmitter.
     */
    suspend fun drainQueue(
        transmitter: suspend (OutboxEntry) -> Boolean
    ): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadFromDiskLocked()
            if (queue.isEmpty()) return@withLock 0

            val now = System.currentTimeMillis()
            val toDrain = queue.values.toList() // oldest-first
            var deliveredCount = 0

            for (entry in toDrain) {
                // Exponential backoff check: delay = min(MAX, BASE * 2^attempts)
                val backoff = minOf(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L shl minOf(entry.attempts, 6)))
                if (entry.lastAttemptAt > 0 && (now - entry.lastAttemptAt) < backoff) {
                    continue // Skip until backoff window expires
                }

                entry.lastAttemptAt = now
                entry.attempts++

                val success = runCatching { transmitter(entry) }.getOrDefault(false)
                if (success) {
                    queue.remove(entry.id)
                    deliveredCount++
                    Log.i(TAG, "[Outbox] Successfully transmitted queued alert: id=${entry.id} tier=${entry.tier}")
                } else {
                    Log.w(TAG, "[Outbox] Retry failed for queued alert: id=${entry.id} attempts=${entry.attempts} nextBackoff=${backoff}ms")
                }
            }

            if (deliveredCount > 0) {
                saveToDiskLocked()
            }
            deliveredCount
        }
    }

    private fun loadFromDiskLocked() {
        if (isLoaded) return
        isLoaded = true
        if (!file.exists() || !file.isFile) return

        try {
            val content = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                OutboxEntry.fromJson(json)?.let { entry ->
                    if (entry.status == OutboxStatus.PENDING) {
                        queue[entry.id] = entry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Outbox] Error reading durable outbox file", e)
        }
    }

    private fun saveToDiskLocked() {
        try {
            file.parentFile?.mkdirs()
            val tempFile = File(file.parentFile, "$OUTBOX_FILENAME.tmp")
            val array = JSONArray()
            queue.values.forEach { entry ->
                array.put(entry.toJson())
            }
            tempFile.writeText(array.toString(), Charsets.UTF_8)
            tempFile.renameTo(file)
        } catch (e: Exception) {
            Log.e(TAG, "[Outbox] Error writing durable outbox file", e)
        }
    }
}
