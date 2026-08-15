package app.openflow.engine

/** Which ear/brain ids are live; stubs resolve away. */
object EarGate {
    private val cloudEars = setOf("openai", "deepgram", "assemblyai", "sarvam")

    fun live(id: String): Boolean {
        val t = id.trim().lowercase()
        return t == "system" || t in cloudEars
    }

    fun resolve(id: String): String {
        val t = id.trim().lowercase()
        return if (live(t)) t else EnginePrefs.DEFAULT_EAR
    }

    fun resolveBrain(id: String): String {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return EnginePrefs.DEFAULT_BRAIN
        if (trimmed.equals("on_phone", ignoreCase = true)) return EnginePrefs.DEFAULT_BRAIN
        return trimmed
    }
}
