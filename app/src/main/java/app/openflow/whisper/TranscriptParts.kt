package app.openflow.whisper

class TranscriptParts {
    private val parts = ArrayList<String>()

    @Synchronized
    fun add(text: String) {
        val t = text.trim()
        if (t.isNotEmpty()) parts.add(t)
    }

    @Synchronized
    fun join(): String = parts.joinToString(" ")
}
