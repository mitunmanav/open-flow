package app.openflow.stt

import app.openflow.engine.EarId
import app.openflow.engine.ProviderId

/**
 * Notice when a cloud ear dies mid-listen (fatal, non-mic).
 * Session is saved via stopListening(save=true) and health already recorded,
 * so the next SttRouter.pick routes around the dead ear. Honest, short copy.
 */
object CloudFallbackNotice {
    val CLOUD: Set<EarId> = setOf(
        EarId.OPENAI,
        EarId.DEEPGRAM,
        EarId.ASSEMBLYAI,
        EarId.SARVAM,
        EarId.CUSTOM_STT,
    )

    const val MESSAGE =
        "Speech service busy. Kept what was heard — try another ear in Speech + AI if it keeps failing."

    fun forFatal(earId: String): String? =
        if (ProviderId.parseEar(earId) in CLOUD) MESSAGE else null
}
