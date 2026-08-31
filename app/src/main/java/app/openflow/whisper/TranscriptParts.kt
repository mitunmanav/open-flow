package app.openflow.whisper

import app.openflow.text.HallucinationGuard

class TranscriptParts {
    private val parts = ArrayList<String>()

    @Volatile
    var last: HallucinationGuard.Result = HallucinationGuard.Result("", false, emptyList())
        private set

    @Synchronized
    fun add(text: String) {
        val t = text.trim()
        if (t.isNotEmpty()) parts.add(t)
    }

    @Synchronized
    fun join(): String {
        val r = HallucinationGuard.apply(parts.joinToString(" "))
        last = r
        return r.text
    }

    @Synchronized
    fun clear() {
        parts.clear()
        last = HallucinationGuard.Result("", false, emptyList())
    }
}
