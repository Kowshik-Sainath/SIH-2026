package com.example.thasmathjagratha.stt.manager

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Robust, resumable HTTP file downloader with:
 * - Chunked streaming to disk (64 KB buffer)
 * - Safe handling when Content-Length is absent (chunked transfer)
 * - Resumable partial downloads via HTTP Range header
 * - Automatic HTTP redirect traversal (up to 10 hops, supporting HTTP -> HTTPS and CDN locations)
 * - Connection and read timeouts (30s connect, 60s read)
 * - Retry with exponential backoff (up to 3 attempts)
 * - Post-download size verification against expected length
 * - Live byte progress reporting and Logcat/telemetry logging
 * - Atomic completion (.partial renamed to destination on verified success)
 */
object ModelDownloader {

    private const val TAG = "ModelDownloader"
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB
    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val MAX_RETRIES = 3
    private const val MAX_REDIRECTS = 10

    suspend fun downloadFile(
        urlStr: String,
        destFile: File,
        expectedSizeBytes: Long? = null,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit = { _, _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (destFile.exists() && destFile.length() > 0L) {
            if (expectedSizeBytes == null || destFile.length() == expectedSizeBytes) {
                Log.i(TAG, "File ${destFile.name} already exists and matches expected size (${destFile.length()} bytes). Skipping download.")
                onProgress(destFile.length(), destFile.length(), 1.0f)
                return@withContext Result.success(Unit)
            }
        }

        destFile.parentFile?.mkdirs()
        val partialFile = File(destFile.parentFile, "${destFile.name}.partial")

        var attempt = 0
        var lastException: Exception? = null

        while (attempt < MAX_RETRIES) {
            attempt++
            try {
                Log.i(TAG, "Download attempt $attempt/$MAX_RETRIES for ${destFile.name} from $urlStr")
                executeDownload(urlStr, destFile, partialFile, expectedSizeBytes, onProgress)
                Log.i(TAG, "Successfully downloaded and verified ${destFile.name} (${destFile.length()} bytes)")
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt $attempt failed for ${destFile.name}: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    val backoffMs = 1500L * attempt
                    Log.i(TAG, "Backing off for ${backoffMs}ms before retry...")
                    delay(backoffMs)
                }
            }
        }

        val error = lastException ?: IOException("Failed to download ${destFile.name} after $MAX_RETRIES attempts")
        Log.e(TAG, "All $MAX_RETRIES download attempts failed for ${destFile.name}", error)
        Result.failure(error)
    }

    private fun executeDownload(
        initialUrl: String,
        destFile: File,
        partialFile: File,
        expectedSizeBytes: Long?,
        onProgress: (Long, Long, Float) -> Unit
    ) {
        val existingBytes = if (partialFile.exists()) partialFile.length() else 0L

        val (connection, finalUrl) = openConnectionWithRedirects(initialUrl, existingBytes)

        val responseCode = connection.responseCode
        val isRangeAccepted = (responseCode == HttpURLConnection.HTTP_PARTIAL)
        val isFullDownload = (responseCode == HttpURLConnection.HTTP_OK)

        if (!isRangeAccepted && !isFullDownload) {
            connection.disconnect()
            if (responseCode == 416) {
                // Requested range not satisfiable (e.g. file changed or corrupted). Reset partial.
                Log.w(TAG, "HTTP 416 Range Not Satisfiable. Resetting partial file.")
                partialFile.delete()
                throw IOException("HTTP 416 Range Not Satisfiable for $finalUrl. Partial file reset.")
            }
            throw IOException("HTTP error $responseCode (${connection.responseMessage}) for $finalUrl")
        }

        val serverContentLength = connection.contentLengthLong
        val totalBytes = when {
            isRangeAccepted && serverContentLength > 0 -> existingBytes + serverContentLength
            isFullDownload && serverContentLength > 0 -> serverContentLength
            expectedSizeBytes != null && expectedSizeBytes > 0 -> expectedSizeBytes
            else -> -1L
        }

        val appendMode = isRangeAccepted && existingBytes > 0L
        if (!appendMode && partialFile.exists()) {
            partialFile.delete()
        }

        var bytesDownloaded = if (appendMode) existingBytes else 0L
        var lastLogTime = System.currentTimeMillis()

        try {
            connection.inputStream.use { input ->
                FileOutputStream(partialFile, appendMode).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastLogTime >= 500) { // Log every 500ms
                            lastLogTime = now
                            val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                            val totalMb = if (totalBytes > 0) String.format("%.1f", totalBytes / (1024.0 * 1024.0)) else "unknown"
                            val downloadedMb = String.format("%.1f", bytesDownloaded / (1024.0 * 1024.0))
                            Log.d(TAG, "[${destFile.name}] $downloadedMb MB / $totalMb MB (${(progress * 100).toInt()}%)")
                            onProgress(bytesDownloaded, totalBytes, progress)
                        }
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }

        // Verify completed byte count
        val finalPartialLength = partialFile.length()
        val expectedLength = if (totalBytes > 0) totalBytes else expectedSizeBytes ?: -1L

        if (expectedLength > 0 && finalPartialLength != expectedLength) {
            Log.e(TAG, "Size mismatch for ${destFile.name}: expected $expectedLength bytes, got $finalPartialLength bytes")
            partialFile.delete()
            throw IOException("Downloaded size mismatch for ${destFile.name}: expected $expectedLength, got $finalPartialLength")
        }

        if (finalPartialLength <= 0L) {
            partialFile.delete()
            throw IOException("Downloaded file ${destFile.name} is empty (0 bytes)")
        }

        // Atomic install
        if (destFile.exists()) {
            destFile.delete()
        }
        val renamed = partialFile.renameTo(destFile)
        if (!renamed) {
            throw IOException("Failed to rename ${partialFile.name} to ${destFile.name}")
        }

        onProgress(destFile.length(), destFile.length(), 1.0f)
    }

    private fun openConnectionWithRedirects(
        urlStr: String,
        rangeStartBytes: Long
    ): Pair<HttpURLConnection, String> {
        var currentUrl = urlStr
        var redirects = 0

        while (redirects < MAX_REDIRECTS) {
            val url = URL(currentUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false // Manage redirects manually to preserve Range headers across hosts
                setRequestProperty("User-Agent", "ThasmathJagratha-Android/1.0 (Linux; Android)")
                if (rangeStartBytes > 0) {
                    setRequestProperty("Range", "bytes=$rangeStartBytes-")
                }
            }

            val status = conn.responseCode
            if (status in 300..399) {
                val newUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (!newUrl.isNullOrBlank()) {
                    currentUrl = if (newUrl.startsWith("http")) newUrl else URL(URL(currentUrl), newUrl).toString()
                    redirects++
                    continue
                }
            }
            return Pair(conn, currentUrl)
        }

        throw IOException("Too many redirects ($MAX_REDIRECTS) for $urlStr")
    }
}
