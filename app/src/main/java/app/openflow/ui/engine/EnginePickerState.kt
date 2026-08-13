package app.openflow.ui.engine

/**
 * Pure picker view-model. String ids only — no engine/ import (f31 owns types).
 * Feature lights duplicate FeatureGate (ids only). Do not wait for FeatureAuto.
 */
data class EnginePreset(val id: String, val label: String)

/** Indicator chip. Lit = this pick turns it on. Not a toggle. */
data class FeatureChip(val id: String, val label: String, val lit: Boolean)

data class EnginePickerState(
    val earId: String,
    val brainId: String,
    val rewrite: Boolean,
    val commandMode: Boolean,
    val livePartials: Boolean,
    val needsKey: Boolean,
    val needsUrl: Boolean,
    val showSarvamMode: Boolean,
    val honesty: String,
    val highLabel: String,
    val commandWhy: String?,
    val chips: List<FeatureChip>,
) {
    companion object {
        val ears: List<EnginePreset> = listOf(
            EnginePreset("system", "Phone STT"),
            EnginePreset("on_phone", "On this phone"),
            EnginePreset("laptop", "Your computer"),
            EnginePreset("openai", "OpenAI"),
            EnginePreset("deepgram", "Deepgram"),
            EnginePreset("assemblyai", "AssemblyAI"),
            EnginePreset("sarvam", "Sarvam"),
            EnginePreset("custom_stt", "Custom speech URL"),
        )

        val brains: List<EnginePreset> = listOf(
            EnginePreset("none", "Rules only"),
            EnginePreset("on_phone", "On this phone"),
            EnginePreset("laptop", "Your computer"),
            EnginePreset("openai", "OpenAI"),
            EnginePreset("grok", "Grok (xAI)"),
            EnginePreset("minimax", "MiniMax"),
            EnginePreset("deepseek", "DeepSeek"),
            EnginePreset("gemini", "Gemini"),
            EnginePreset("mistral", "Mistral"),
            EnginePreset("together", "Together"),
            EnginePreset("fireworks", "Fireworks"),
            EnginePreset("openrouter", "OpenRouter"),
            EnginePreset("sarvam", "Sarvam"),
            EnginePreset("anthropic", "Anthropic"),
            EnginePreset("custom", "Custom URL"),
        )

        val sarvamModes: List<EnginePreset> = listOf(
            EnginePreset("transcribe", "Transcribe"),
            EnginePreset("translate", "Translate"),
            EnginePreset("mix", "Mix"),
            EnginePreset("roman", "Roman"),
        )

        private val knownEars = ears.map { it.id }.toSet()
        private val knownBrains = brains.map { it.id }.toSet()

        private val keyEars = setOf("openai", "deepgram", "assemblyai", "sarvam", "custom_stt")
        private val keyBrains = setOf(
            "openai", "grok", "minimax", "deepseek", "gemini", "mistral",
            "together", "fireworks", "openrouter", "sarvam", "anthropic", "custom",
        )
        private val urlEars = setOf("laptop", "custom_stt")
        private val urlBrains = setOf("laptop", "custom")
        private val rewriteBrains = knownBrains - setOf("none", "on_phone")

        fun of(earId: String = "system", brainId: String = "none"): EnginePickerState {
            val ear = if (earId in knownEars) earId else "system"
            val brain = if (brainId in knownBrains) brainId else "none"
            val rewrite = brain in rewriteBrains
            val livePartials = ear in knownEars
            val showSarvam = ear == "sarvam"
            return EnginePickerState(
                earId = ear,
                brainId = brain,
                rewrite = rewrite,
                commandMode = rewrite,
                livePartials = livePartials,
                needsKey = ear in keyEars || brain in keyBrains,
                needsUrl = ear in urlEars || brain in urlBrains,
                showSarvamMode = showSarvam,
                honesty = honestyLine(ear, brain),
                highLabel = if (rewrite) "High (AI)" else "High (rules)",
                commandWhy = if (rewrite) null else "Command Mode — needs a brain",
                chips = listOf(
                    FeatureChip("high_ai", if (rewrite) "High AI" else "High (rules)", rewrite),
                    FeatureChip("command", "Command", rewrite),
                    FeatureChip("live_partials", "live partials", livePartials),
                    FeatureChip("sarvam", "Sarvam modes", showSarvam),
                ),
            )
        }

        fun maskKey(key: String): String {
            val t = key.trim()
            if (t.isEmpty()) return ""
            if (t.length < 4) return "••••"
            return "••••" + t.takeLast(4)
        }

        private fun honestyLine(ear: String, brain: String): String {
            when (ear) {
                "openai" -> return "Your voice goes to OpenAI."
                "deepgram" -> return "Your voice goes to Deepgram."
                "assemblyai" -> return "Your voice goes to AssemblyAI."
                "sarvam" -> return "Your voice goes to Sarvam."
                "custom_stt" -> return "Your voice goes to the speech URL you set."
                "laptop" -> return "Audio goes to your computer."
            }
            when (brain) {
                "grok" -> return "Text of this utterance goes to xAI (Grok). Not Groq."
                "openai" -> return "Text of this utterance goes to OpenAI."
                "minimax" -> return "Text of this utterance goes to MiniMax."
                "deepseek" -> return "Text of this utterance goes to DeepSeek."
                "gemini" -> return "Text of this utterance goes to Google (Gemini)."
                "mistral" -> return "Text of this utterance goes to Mistral."
                "together" -> return "Text of this utterance goes to Together."
                "fireworks" -> return "Text of this utterance goes to Fireworks."
                "openrouter" -> return "Text of this utterance goes to OpenRouter."
                "sarvam" -> return "Text of this utterance goes to Sarvam."
                "anthropic" -> return "Text of this utterance goes to Anthropic."
                "custom" -> return "Text of this utterance goes to the URL you set."
                "laptop" -> return "Text goes to the computer you set."
            }
            return if (ear == "on_phone") {
                "Audio stays on this phone."
            } else {
                "On this phone. Phone STT may still use Google."
            }
        }
    }
}
