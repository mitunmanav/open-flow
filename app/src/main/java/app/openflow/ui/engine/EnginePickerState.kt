package app.openflow.ui.engine

import app.openflow.ai.providers.host.HostUrl
import app.openflow.engine.EarGate

/**
 * Pure picker view-model. String ids only — no engine/ import (f31 owns types).
 * Feature lights duplicate FeatureGate (ids only). Do not wait for FeatureAuto.
 */
data class EnginePreset(val id: String, val label: String)

/** Picker group: Local / Cloud / Later. */
data class EngineSection(val id: String, val title: String, val items: List<EnginePreset>)

/** Indicator chip. Lit = this pick turns it on. Not a toggle. */
data class FeatureChip(val id: String, val label: String, val lit: Boolean)

data class EnginePickerState(
    val earId: String,
    val brainId: String,
    val autoRoute: Boolean,
    val rewrite: Boolean,
    val commandMode: Boolean,
    val livePartials: Boolean,
    val needsKey: Boolean,
    val needsEarKey: Boolean = false,
    val needsBrainKey: Boolean = false,
    val needsUrl: Boolean,
    val showSarvamMode: Boolean,
    val pathKind: String,
    val honesty: String,
    val highLabel: String,
    val commandWhy: String?,
    val chips: List<FeatureChip>,
) {
    companion object {
        val ears: List<EnginePreset> = listOf(
            EnginePreset("system", "Phone speech"),
            EnginePreset("on_phone", "Whisper on phone"),
            EnginePreset("laptop", "Your computer"),
            EnginePreset("openai", "OpenAI"),
            EnginePreset("deepgram", "Deepgram"),
            EnginePreset("assemblyai", "AssemblyAI"),
            EnginePreset("sarvam", "Sarvam"),
            EnginePreset("custom_stt", "Custom speech URL"),
        )

        val brains: List<EnginePreset> = listOf(
            EnginePreset("none", "Rules only"),
            EnginePreset("on_phone", "On-device model"),
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
            EnginePreset("verbatim", "Verbatim"),
            EnginePreset("translit", "Translit"),
            EnginePreset("codemix", "Code-mix"),
        )

        fun earSections(): List<EngineSection> = listOf(
            EngineSection(
                "local",
                "On this phone",
                listOf(EnginePreset("system", "Phone speech")),
            ),
            EngineSection(
                "cloud",
                "Cloud speech",
                listOf(
                    EnginePreset("openai", "OpenAI"),
                    EnginePreset("deepgram", "Deepgram"),
                    EnginePreset("assemblyai", "AssemblyAI"),
                    EnginePreset("sarvam", "Sarvam"),
                ),
            ),
            EngineSection(
                "later",
                "Coming later",
                listOf(
                    EnginePreset("on_phone", "Whisper on phone"),
                    EnginePreset("laptop", "Your computer"),
                    EnginePreset("custom_stt", "Custom speech URL"),
                ),
            ),
        )

        fun brainSections(): List<EngineSection> = listOf(
            EngineSection(
                "rules",
                "No AI rewrite",
                listOf(EnginePreset("none", "Rules only")),
            ),
            EngineSection(
                "cloud",
                "Cloud rewrite",
                listOf(
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
                ),
            ),
            EngineSection(
                "later",
                "Coming later",
                listOf(
                    EnginePreset("on_phone", "On-device model"),
                    EnginePreset("laptop", "Your computer"),
                    EnginePreset("custom", "Custom URL"),
                ),
            ),
        )

        private val knownEars = ears.map { it.id }.toSet()
        private val knownBrains = brains.map { it.id }.toSet()

        private val keyEars = setOf("openai", "deepgram", "assemblyai", "sarvam", "custom_stt")
        private val keyBrains = setOf(
            "openai", "grok", "minimax", "deepseek", "gemini", "mistral",
            "together", "fireworks", "openrouter", "sarvam", "anthropic", "custom",
            "laptop",
        )
        private val urlEars = setOf("laptop", "custom_stt")
        private val urlBrains = setOf("laptop", "custom")
        private val rewriteBrains = knownBrains - setOf("none", "on_phone")

        const val STUB_EAR_REASON = "Not ready yet — use Phone speech or a cloud option with a key"

        /** Shown on ear/brain pickers when [autoRoute] is ON — still editable for Manual fallback. */
        const val OVERRIDE_MANUAL_FALLBACK = "Override (Manual fallback)"

        fun manualFallbackHint(autoRoute: Boolean): String? =
            if (autoRoute) OVERRIDE_MANUAL_FALLBACK else null

        fun earEnabled(id: String): Boolean = EarGate.live(id)

        fun earDisabledReason(id: String): String? =
            if (earEnabled(id)) null else STUB_EAR_REASON

        fun brainEnabled(id: String, url: String): Boolean = when (id) {
            "none" -> true
            "on_phone" -> false
            "laptop", "custom" -> HostUrl.allow(url)
            else -> id in knownBrains
        }

        fun brainDisabledReason(id: String, url: String): String? = when {
            brainEnabled(id, url) -> null
            id == "on_phone" -> "Not in this version — on-device rewrite comes later"
            id in urlBrains -> "Add a valid HTTPS or LAN URL first"
            else -> null
        }

        fun of(
            earId: String = "system",
            brainId: String = "none",
            autoRoute: Boolean = false,
        ): EnginePickerState {
            val ear = if (earId in knownEars) earId else "system"
            val brain = if (brainId in knownBrains) brainId else "none"
            val rewrite = brain in rewriteBrains
            val livePartials = ear in knownEars
            val showSarvam = ear == "sarvam"
            val kind = pathKind(ear, brain)
            val earKey = ear in keyEars
            val brainKey = brain in keyBrains
            return EnginePickerState(
                earId = ear,
                brainId = brain,
                autoRoute = autoRoute,
                rewrite = rewrite,
                commandMode = rewrite,
                livePartials = livePartials,
                needsKey = earKey || brainKey,
                needsEarKey = earKey,
                needsBrainKey = brainKey,
                needsUrl = ear in urlEars || brain in urlBrains,
                showSarvamMode = showSarvam,
                pathKind = kind,
                honesty = "$kind. ${honestyLine(ear, brain)}",
                highLabel = if (rewrite) "High (AI)" else "High (rules)",
                commandWhy = if (rewrite) null else "Voice commands need a rewrite brain",
                chips = listOf(
                    FeatureChip("high_ai", if (rewrite) "High AI" else "High (rules)", rewrite),
                    FeatureChip("command", "Commands", rewrite),
                    FeatureChip("live_partials", "Live partials", livePartials),
                    FeatureChip("sarvam", "Sarvam modes", showSarvam),
                ),
            )
        }

        fun pathKind(ear: String, brain: String): String {
            val onlineEar = ear in setOf("openai", "deepgram", "assemblyai", "sarvam", "laptop", "custom_stt")
            val onlineBrain = brain in rewriteBrains
            return if (onlineEar || onlineBrain) "Online" else "Local"
        }

        fun maskKey(key: String): String {
            val t = key.trim()
            if (t.isEmpty()) return ""
            if (t.length < 4) return "••••"
            return "••••" + t.takeLast(4)
        }

        fun missingKeyLine(needsKey: Boolean, keyMask: String): String? =
            if (needsKey && keyMask.isBlank()) {
                "Add an API key below — this choice needs the network."
            } else {
                null
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
                "On this phone. Phone speech may still use Google."
            }
        }
    }
}
