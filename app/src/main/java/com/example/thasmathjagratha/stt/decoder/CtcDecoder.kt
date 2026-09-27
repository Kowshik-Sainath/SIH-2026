package com.example.thasmathjagratha.stt.decoder

import org.json.JSONObject
import java.io.File

/**
 * CTC Decoder supporting argmax greedy search, repeat token merging, blank token stripping,
 * vocabulary map lookup, and domain phrase biasing.
 */
class CtcDecoder(
    val vocabMap: Map<Int, String>,
    val blankId: Int = 0,
    val languageCode: String = "hi"
) {

    /**
     * Decode 2D logit array (numFrames x vocabSize) into plain text string.
     */
    fun decode(logits: Array<FloatArray>): String {
        if (logits.isEmpty()) return ""

        val rawTokenIds = ArrayList<Int>(logits.size)

        for (frameLogits in logits) {
            var maxIdx = 0
            var maxVal = Float.NEGATIVE_INFINITY

            for (i in frameLogits.indices) {
                var logitVal = frameLogits[i]
                val tokenStr = vocabMap[i].orEmpty()
                if (tokenStr.isNotEmpty()) {
                    val boost = DomainPhraseBiasing.getBoostForToken(tokenStr, languageCode)
                    logitVal += boost
                }

                if (logitVal > maxVal) {
                    maxVal = logitVal
                    maxIdx = i
                }
            }
            rawTokenIds.add(maxIdx)
        }

        // CTC collapse repeat tokens and strip blanks
        val collapsed = ArrayList<Int>()
        var prevId = -1

        for (id in rawTokenIds) {
            if (id != blankId && id != prevId) {
                collapsed.add(id)
            }
            prevId = id
        }

        val sb = StringBuilder()
        for (id in collapsed) {
            val token = vocabMap[id] ?: continue
            if (token == "<blank>" || token == "<pad>" || token == "<s>" || token == "</s>" || token == "<unk>") {
                continue
            }
            if (token.startsWith(" ")) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(token.substring(1))
            } else if (token.startsWith("##")) {
                sb.append(token.substring(2))
            } else {
                sb.append(token)
            }
        }

        return sb.toString().trim()
    }

    companion object {
        /**
         * Load vocabulary dictionary from tokens.txt or vocab.json.
         */
        fun loadVocab(tokensFile: File): Map<Int, String> {
            val map = HashMap<Int, String>()
            if (!tokensFile.exists()) return map

            try {
                val content = tokensFile.readText()
                if (tokensFile.name.endsWith(".json")) {
                    val jsonObj = JSONObject(content)
                    val keys = jsonObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val valIdx = jsonObj.optInt(key, -1)
                        if (valIdx >= 0) {
                            map[valIdx] = key
                        }
                    }
                } else {
                    // txt format: each line "token index" or "token"
                    val lines = content.lines()
                    for ((idx, line) in lines.withIndex()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2 && parts.last().toIntOrNull() != null) {
                            val token = parts.dropLast(1).joinToString(" ")
                            val tokenIdx = parts.last().toInt()
                            map[tokenIdx] = token
                        } else {
                            map[idx] = trimmed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return map
        }
    }
}
