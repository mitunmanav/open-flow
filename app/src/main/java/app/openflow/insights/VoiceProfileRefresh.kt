package app.openflow.insights

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import java.util.TimeZone

/**
 * User-tap BYOK Voice flavor. Aggregates only — never transcripts.
 */
object VoiceProfileRefresh {
    suspend fun run(
        sessions: List<InsightSession>,
        totalWords: Long,
        streakDays: Int,
        zone: TimeZone,
        brain: TextAIProvider,
        providerName: String,
        modelName: String = "",
    ): Result<VoiceFlavor> {
        if (!InsightsAggregatePolicy.voiceUnlocked(totalWords)) {
            return Result.failure(IllegalStateException("Need ${InsightsAggregatePolicy.VOICE_UNLOCK_WORDS} words first"))
        }
        if (brain === NoAI || brain.name.equals("none", ignoreCase = true)) {
            return Result.failure(IllegalStateException("Add a brain key in Speech + AI"))
        }
        val payload = InsightsAggregatePolicy.byokPayload(
            sessions = sessions,
            totalWords = totalWords,
            streakDays = streakDays,
            zone = zone,
        )
        val msg = VoiceProfilePrompt.buildUserMessage(payload)
        return try {
            val out = brain.enhance(msg, mode = "voice_profile")
            val flavor = VoiceProfilePrompt.parseFlavor(out)
                ?: return Result.failure(IllegalStateException("Brain reply was not valid Voice JSON"))
            Result.success(flavor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
