package app.openflow.orchestrate

object BrainRouter {
    @Suppress("UNUSED_PARAMETER")
    fun pick(
        auto: Boolean,
        manualBrainId: String,
        signals: RouteSignals,
        health: ProviderHealth,
        looksLikeCommand: Boolean,
        textLen: Int,
        candidates: List<String> = listOf("none", "openai", "anthropic", "grok", "gemini"),
    ): RouteExplain {
        return BrainHop.pick(
            BrainHopAsk(
                mode = if (auto) RouteMode.LOCAL_THEN_AI else RouteMode.LOCAL_ONLY,
                aiWhen = AiWhen.EVERY,
                brainId = manualBrainId,
                signals = signals,
                looksLikeCommand = looksLikeCommand,
                textLen = textLen,
                cleaned = "x".repeat(textLen.coerceAtLeast(0)),
                levelRaw = false,
            )
        )
    }
}
