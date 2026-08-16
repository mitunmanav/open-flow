package app.openflow.engine

import app.openflow.secrets.SecretStore
import app.openflow.text.Feature
import app.openflow.text.FeatureAuto

/** Persist pick + keys. UI calls these by name. */
class EngineSession(
    private val prefs: EnginePrefs,
    private val secrets: SecretStore,
) {
    fun pick(ear: String, brain: String) {
        prefs.earId = ear
        prefs.brainId = brain
    }

    /** Write every key id the current pick needs. Empty clears those ids. */
    fun saveKey(key: String) {
        val value = key.trim()
        for (id in keyIdsFor(prefs.earId, prefs.brainId)) {
            secrets.put(id, value)
        }
    }

    fun saveEarKey(key: String) {
        val value = key.trim()
        val e = prefs.earId.trim().lowercase()
        if (e in EAR_KEY_IDS) {
            secrets.put(e, value)
        }
    }

    fun saveBrainKey(key: String) {
        val value = key.trim()
        val b = prefs.brainId.trim().lowercase()
        if (b in BRAIN_KEY_IDS) {
            secrets.put(b, value)
        }
    }

    fun saveUrl(url: String) {
        prefs.customBaseUrl = url
    }

    fun saveSarvam(mode: String) {
        prefs.sarvamMode = mode
    }

    /** ••••last4, or "" if no key. */
    fun keyMask(): String {
        val raw = keyIdsFor(prefs.earId, prefs.brainId)
            .firstNotNullOfOrNull { secrets.get(it) }
            .orEmpty()
        return maskKey(raw)
    }

    fun earKeyMask(): String {
        val e = prefs.earId.trim().lowercase()
        if (e !in EAR_KEY_IDS) return ""
        val raw = secrets.get(e).orEmpty()
        return maskKey(raw)
    }

    fun brainKeyMask(): String {
        val b = prefs.brainId.trim().lowercase()
        if (b !in BRAIN_KEY_IDS) return ""
        val raw = secrets.get(b).orEmpty()
        return maskKey(raw)
    }

    fun features(): Set<Feature> = FeatureAuto.of(prefs.earId, prefs.brainId)

    companion object {
        private val EAR_KEY_IDS = setOf(
            "openai", "deepgram", "assemblyai", "sarvam", "custom_stt",
        )
        private val BRAIN_KEY_IDS = setOf(
            "openai", "grok", "minimax", "deepseek", "gemini", "mistral",
            "together", "fireworks", "openrouter", "sarvam", "anthropic",
            "custom", "laptop",
        )

        fun keyIdsFor(ear: String, brain: String): Set<String> {
            val ids = linkedSetOf<String>()
            val e = ear.trim().lowercase()
            val b = brain.trim().lowercase()
            if (e in EAR_KEY_IDS) ids.add(e)
            if (b in BRAIN_KEY_IDS) ids.add(b)
            return ids
        }

        fun maskKey(key: String): String {
            val t = key.trim()
            if (t.isEmpty()) return ""
            if (t.length < 4) return "••••"
            return "••••" + t.takeLast(4)
        }
    }
}
