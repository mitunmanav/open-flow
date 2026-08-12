package app.openflow.ai

/**
 * Optional text enhancement after STT (cleanup, rewrite, tone).
 * MVP uses [NoAI] — identity pass-through, zero network, on-device only product path.
 */
interface TextAIProvider {
    /** Human-readable provider id (prefs / privacy report). */
    val name: String

    /**
     * Enhance [text]. [mode] is a free-form hint (e.g. "cleanup", "formal").
     * Implementations must not throw for normal empty/short input; return [text] or improved text.
     */
    suspend fun enhance(text: String, mode: String = "cleanup"): String
}

/**
 * Default provider: no model, no network. Returns text unchanged.
 */
object NoAI : TextAIProvider {
    override val name: String = "none"

    override suspend fun enhance(text: String, mode: String): String = text
}

/**
 * FUTURE: on-device LLM (e.g. MediaPipe / Gemma / local GGUF).
 * Not wired. Throws until implemented.
 */
class LocalLLM(
    private val modelId: String = "unset"
) : TextAIProvider {
    override val name: String = "local:$modelId"

    override suspend fun enhance(text: String, mode: String): String {
        throw UnsupportedOperationException(
            "LocalLLM not implemented (FUTURE). modelId=$modelId mode=$mode"
        )
    }
}

/**
 * FUTURE: remote API LLM. Product default stays offline; this is opt-in only.
 * Not wired. Throws until implemented.
 */
class RemoteLLM(
    private val endpointLabel: String = "unset"
) : TextAIProvider {
    override val name: String = "remote:$endpointLabel"

    override suspend fun enhance(text: String, mode: String): String {
        throw UnsupportedOperationException(
            "RemoteLLM not implemented (FUTURE). endpoint=$endpointLabel mode=$mode"
        )
    }
}
