package app.openflow.engine

/**
 * Live picks for development / opt-in cloud.
 * Stub ears (on-phone / laptop / custom_stt) stay gated until Track B/C.
 */
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
        val b = id.trim().lowercase()
        return if (b == "on_phone") EnginePrefs.DEFAULT_BRAIN else id.trim().ifEmpty {
            EnginePrefs.DEFAULT_BRAIN
        }
    }
}
