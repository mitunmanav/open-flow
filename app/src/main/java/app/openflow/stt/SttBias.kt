package app.openflow.stt

import app.openflow.text.LearnEngine

/**
 * Bias strings for SpeechRecognizer (API 33+).
 * Dictionary + focused-field tokens only. No scrape of other apps.
 */
object SttBias {
    const val MAX = 80

    fun strings(dictionary: Map<String, String>, fieldTokens: List<String>): List<String> {
        val raw = ArrayList<String>(dictionary.size * 2 + fieldTokens.size)
        dictionary.forEach { (k, v) ->
            raw.add(k)
            raw.add(v)
        }
        raw.addAll(fieldTokens)
        val cleaned = raw.map { it.trim() }.filter { keep(it) }.distinct()
        if (cleaned.size <= MAX) return cleaned
        return cleaned.sortedByDescending { it.length }.take(MAX)
    }

    fun fieldTokens(fieldText: String): List<String> =
        fieldText.split(Regex("\\s+")).map { it.trim() }.filter { keep(it) }

    private fun keep(token: String): Boolean {
        if (token.length < 2) return false
        if (token.lowercase() in LearnEngine.COMMON) return false
        return true
    }
}
