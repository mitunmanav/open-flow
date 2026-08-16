package app.openflow.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.OpenFlowApp
import app.openflow.data.VoiceProfileEntity
import app.openflow.insights.InsightSession
import app.openflow.insights.InsightsAggregatePolicy
import app.openflow.insights.VoiceProfileRefresh
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.privacy.PrivacyHonesty
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    app: OpenFlowApp,
    onOpenSpeechAi: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var sessions by remember { mutableStateOf<List<InsightSession>>(emptyList()) }
    var totalWords by remember { mutableStateOf(0L) }
    var totalSessions by remember { mutableStateOf(0L) }
    var streak by remember { mutableIntStateOf(0) }
    var voice by remember { mutableStateOf<VoiceProfileEntity?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val zone = remember { TimeZone.getDefault() }

    fun reload() {
        scope.launch {
            val stats = app.dictations.stats()
            totalWords = stats.totalWords
            totalSessions = stats.totalSessions
            streak = stats.streakDays
            sessions = app.dictations.allForInsights().map {
                InsightSession(
                    text = it.text,
                    rawText = it.rawText,
                    createdAtEpochMs = it.createdAtEpochMs,
                    durationMs = it.durationMs,
                    wordCount = it.wordCount,
                    packageName = it.packageName,
                )
            }
            voice = app.dictations.voiceProfile()
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimen.MIN_PADDING)
            .testTag("insights_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OpenChip(
                label = "Usage",
                isOn = tab == 0,
                onClick = { tab = 0 },
                modifier = Modifier.testTag("insights_tab_usage"),
            )
            OpenChip(
                label = "Voice",
                isOn = tab == 1,
                onClick = { tab = 1 },
                modifier = Modifier.testTag("insights_tab_voice"),
            )
        }

        if (tab == 0) {
            UsagePane(
                totalWords = totalWords,
                totalSessions = totalSessions,
                streak = streak,
                sessions = sessions,
                zone = zone,
            )
        } else {
            VoicePane(
                totalWords = totalWords,
                sessions = sessions,
                zone = zone,
                voice = voice,
                busy = busy,
                error = error,
                onOpenSpeechAi = onOpenSpeechAi,
                onRefresh = {
                    scope.launch {
                        busy = true
                        error = null
                        val brain = app.currentBrain()
                        val result = VoiceProfileRefresh.run(
                            sessions = sessions,
                            totalWords = totalWords,
                            streakDays = streak,
                            zone = zone,
                            brain = brain,
                            providerName = brain.name,
                        )
                        result.onSuccess { flavor ->
                            val row = VoiceProfileEntity(
                                archetype = flavor.archetype,
                                catchphrase = flavor.catchphrase,
                                headline = flavor.headline,
                                generatedAtEpochMs = System.currentTimeMillis(),
                                provider = brain.name,
                                model = "",
                            )
                            app.dictations.saveVoiceProfile(row)
                            voice = row
                        }.onFailure {
                            error = it.message ?: "Refresh failed"
                        }
                        busy = false
                    }
                },
            )
        }
    }
}

@Composable
private fun UsagePane(
    totalWords: Long,
    totalSessions: Long,
    streak: Int,
    sessions: List<InsightSession>,
    zone: TimeZone,
) {
    val wpm = InsightsAggregatePolicy.wordsPerMinute(sessions)
    val cleaned = InsightsAggregatePolicy.cleanedDeltaWords(sessions)
    val days = InsightsAggregatePolicy.dayWordCounts(
        sessions,
        nowMs = System.currentTimeMillis(),
        zone = zone,
        weeks = 12,
    )
    val topApp = InsightsAggregatePolicy.topPackage(sessions)

    Tile("Words", "$totalWords")
    Tile("Sessions", "$totalSessions")
    Tile("WPM", String.format(Locale.US, "%.1f", wpm))
    Tile("Streak", "$streak d")
    Tile("Cleaned by rules", "$cleaned")

    OpenCard(modifier = Modifier.testTag("insights_heatmap")) {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Last 12 weeks", fontWeight = FontWeight.Bold)
            HeatmapGrid(days = days, streakDays = streak)
        }
    }

    OpenCard {
        Column(Modifier.padding(Dimen.MIN_PADDING), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Top app", fontWeight = FontWeight.Bold)
            Text(topApp ?: "— (new sessions only)")
        }
    }
}

@Composable
private fun VoicePane(
    totalWords: Long,
    sessions: List<InsightSession>,
    zone: TimeZone,
    voice: VoiceProfileEntity?,
    busy: Boolean,
    error: String?,
    onOpenSpeechAi: () -> Unit,
    onRefresh: () -> Unit,
) {
    val unlocked = InsightsAggregatePolicy.voiceUnlocked(totalWords)
    val peak = InsightsAggregatePolicy.peakHour(sessions, zone)
    val topWord = InsightsAggregatePolicy.topWord(sessions)
    val corrected = InsightsAggregatePolicy.mostCorrectedToken(sessions)
    val topApp = InsightsAggregatePolicy.topPackage(sessions)

    Tile("Peak hour", peak?.let { String.format(Locale.US, "%02d:00", it) } ?: "—")
    Tile("Top word", topWord ?: "—")
    Tile("Most corrected", corrected ?: "—")
    Tile("Top app", topApp ?: "—")

    if (voice != null && voice.archetype.isNotBlank()) {
        OpenCard(modifier = Modifier.testTag("insights_voice_flavor")) {
            Column(Modifier.padding(Dimen.MIN_PADDING), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(voice.archetype, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(voice.headline)
                Text("“${voice.catchphrase}”")
            }
        }
    }

    val progress = totalWords.coerceAtMost(InsightsAggregatePolicy.VOICE_UNLOCK_WORDS)
    Text(
        if (unlocked) "Voice refresh unlocked"
        else "Unlock refresh: $progress / ${InsightsAggregatePolicy.VOICE_UNLOCK_WORDS}",
        modifier = Modifier.testTag("insights_voice_unlock"),
    )

    OpenButton(
        text = if (busy) "Refreshing…" else "Refresh with BYOK",
        onClick = onRefresh,
        enabled = unlocked && !busy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_voice_refresh"),
    )

    OpenChip(
        label = "Speech + AI keys",
        isOn = false,
        onClick = onOpenSpeechAi,
        modifier = Modifier.testTag("insights_open_speech_ai"),
    )

    Text(
        PrivacyHonesty.INSIGHTS_VOICE,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Tile(label: String, value: String) {
    OpenCard {
        Column(Modifier.padding(Dimen.MIN_PADDING), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeatmapGrid(days: List<app.openflow.insights.DayBucket>, streakDays: Int) {
    val max = days.maxOfOrNull { it.words }?.coerceAtLeast(1) ?: 1
    val glow = streakDays > 1
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        days.chunked(7).forEach { week ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                week.forEach { day ->
                    val t = day.words.toFloat() / max
                    val bg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f + 0.55f * t)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(bg)
                            .then(
                                if (glow && day.words > 0) {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                                } else Modifier
                            ),
                    )
                }
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
