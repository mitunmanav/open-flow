package app.openflow.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.openflow.OpenFlowApp
import app.openflow.orchestrate.SharePayload
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.privacy.PrivacyHonesty
import kotlinx.coroutines.launch
import java.util.TimeZone

/**
 * Wispr-shaped Home feed (brutalism): banners → stats pager → note → search → day history.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeFeed(
    app: OpenFlowApp,
    bubbleOn: Boolean,
    micOn: Boolean,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit,
    onOpenInsights: () -> Unit = {},
    dictationCard: @Composable (
        d: app.openflow.data.DictationEntity,
        onDelete: () -> Unit,
        onShare: () -> Unit,
        onSave: (String, String) -> Unit,
        onUseRaw: (String) -> Unit,
    ) -> Unit,
    useHistoryRaw: (android.content.Context, String) -> Unit,
) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var words by remember { mutableStateOf(0L) }
    var sessions by remember { mutableStateOf(0L) }
    var streak by remember { mutableStateOf(0) }
    var statsPage by remember { mutableIntStateOf(0) }
    var localNote by rememberSaveable { mutableStateOf(app.prefs.homeNote) }
    var snoozed by remember { mutableStateOf(app.prefs.isSnoozed()) }
    var seenHowTo by remember { mutableStateOf(app.prefs.seenHowTo) }
    var homeSearch by rememberSaveable { mutableStateOf("") }
    val owner = LocalLifecycleOwner.current

    fun refreshStats() {
        scope.launch {
            val s = app.dictations.stats()
            words = s.totalWords
            sessions = s.totalSessions
            streak = s.streakDays
        }
    }

    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                snoozed = app.prefs.isSnoozed()
                refreshStats()
            }
        }
        owner.lifecycle.addObserver(obs)
        refreshStats()
        onDispose {
            owner.lifecycle.removeObserver(obs)
            app.prefs.homeNote = localNote
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = HomeFeedTokens.pagePadH, vertical = HomeFeedTokens.pagePadV)
            .verticalScroll(rememberScrollState())
            .testTag("home_hub"),
        verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.sectionGap)
    ) {
        if (!seenHowTo) {
            OpenCard(modifier = Modifier.testTag("home_howto")) {
                Column(
                    Modifier
                        .padding(Dimen.MIN_PADDING)
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap)
                ) {
                    Text(
                        "How Open Flow works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                    Text(
                        "Not a keyboard. Keep yours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                    Text(
                        "Tap the bubble, then tap again to insert.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                    Text(
                        "X cancel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                    Text(
                        "Dict = one word. Snippet = whole block.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                    OpenButton(
                        text = "Got it",
                        onClick = {
                            app.prefs.seenHowTo = true
                            seenHowTo = true
                        },
                        modifier = Modifier.testTag("home_howto_got_it")
                    )
                }
            }
        }

        when (val banner = HomeBannerPolicy.banner(bubbleOn = bubbleOn, micOn = micOn, snoozed = snoozed)) {
            HomeBannerPolicy.Banner.REPAIR_A11Y -> {
                val copy = HomeBannerPolicy.copy(banner)
                OpenCard(modifier = Modifier.testTag("home_banner_repair")) {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap)
                    ) {
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                        if (copy.body != null) {
                            Text(
                                copy.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                        OpenButton(
                            text = copy.cta ?: "Open Accessibility",
                            onClick = onEnableBubble,
                            contentDescription = copy.a11yLabel,
                            modifier = Modifier.testTag("home_banner_a11y")
                        )
                    }
                }
            }
            HomeBannerPolicy.Banner.ALLOW_MIC -> {
                val copy = HomeBannerPolicy.copy(banner)
                OpenCard(modifier = Modifier.testTag("home_banner_mic")) {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap)
                    ) {
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                        if (copy.body != null) {
                            Text(
                                copy.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                        OpenButton(
                            text = copy.cta ?: "Allow microphone",
                            onClick = onMic,
                            contentDescription = copy.a11yLabel,
                            modifier = Modifier.testTag("home_banner_mic_btn")
                        )
                    }
                }
            }
            HomeBannerPolicy.Banner.END_SNOOZE -> {
                val copy = HomeBannerPolicy.copy(banner)
                OpenCard(modifier = Modifier.testTag("home_banner_snooze")) {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap)
                    ) {
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                        OpenButton(
                            text = copy.cta ?: "End snooze",
                            onClick = {
                                app.prefs.clearSnooze()
                                snoozed = false
                                Toast.makeText(ctx, "Snooze ended", Toast.LENGTH_SHORT).show()
                            },
                            contentDescription = copy.a11yLabel,
                            modifier = Modifier.testTag("home_banner_end_snooze")
                        )
                    }
                }
            }
            HomeBannerPolicy.Banner.NONE -> Unit
        }

        // Stats pager (chip pages — no new pager dep)
        OpenCard(modifier = Modifier.testTag("home_stats")) {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap)
            ) {
                val pages = listOf(
                    "$words words" to "Words",
                    "$sessions sessions" to "Sessions",
                    "$streak d streak" to "Streak",
                )
                Text(
                    pages[statsPage.coerceIn(0, pages.lastIndex)].first,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_stats_value")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HomeFeedTokens.chipGap),
                    verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.chipGap),
                    modifier = Modifier.testTag("home_stats_pages")
                ) {
                    pages.forEachIndexed { i, (_, label) ->
                        OpenChip(
                            label = label,
                            isOn = statsPage == i,
                            onClick = { statsPage = i },
                            modifier = Modifier.testTag("home_stats_page_$i")
                        )
                    }
                }
            }
        }

        OpenChip(
            label = "Open Insights",
            isOn = false,
            onClick = onOpenInsights,
            modifier = Modifier.testTag("home_open_insights"),
        )

        OpenCard(modifier = Modifier.testTag("home_local_note")) {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap)
            ) {
                Text(
                    "Local note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OpenTextField(
                    value = localNote,
                    onValueChange = {
                        localNote = it
                        app.prefs.homeNote = it
                    },
                    placeholder = "Scratch on this device…",
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.testTag("home_note_field")
                )
            }
        }

        OpenTextField(
            value = homeSearch,
            onValueChange = { homeSearch = it },
            placeholder = "Search transcripts…",
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.testTag("home_history_search")
        )

        val shown = dictations.filter {
            HubListPolicy.matches(homeSearch, it.text, it.rawText)
        }
        if (shown.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = if (homeSearch.isBlank()) "No dictations yet" else "No matching results",
                subtitle = if (homeSearch.isBlank()) {
                    "Use the floating bubble in any app to build private history."
                } else {
                    "Try a different search keyword."
                },
                modifier = Modifier.testTag("home_recent_empty")
            )
        } else {
            val nowMs = System.currentTimeMillis()
            val days = HistoryDays.group(
                shown.map { HistoryDays.Row(it.id, it.createdAtEpochMs, it.text) },
                nowMs = nowMs,
                zoneOffsetMs = TimeZone.getDefault().getOffset(nowMs).toLong()
            )
            val byId = shown.associateBy { it.id }
            days.forEach { day ->
                Text(
                    day.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("home_day_${day.label}")
                )
                day.rows.forEach { row ->
                    val d = byId[row.id] ?: return@forEach
                    dictationCard(
                        d,
                        { scope.launch { app.dictations.deleteDictation(d.id) } },
                        {
                            val body = SharePayload.forRow(d.text, d.rawText)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                            try {
                                ctx.startActivity(Intent.createChooser(send, "Share dictation"))
                            } catch (_: Exception) {
                            }
                        },
                        { old, new ->
                            scope.launch {
                                if (app.prefs.autoLearn) {
                                    app.dictations.learnFromEdit(old, new)
                                }
                                app.dictations.updateDictationText(d.id, new)
                            }
                        },
                        { raw -> useHistoryRaw(ctx, raw) },
                    )
                }
            }
        }

        Text(
            PrivacyHonesty.HOME_FOOTER,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            softWrap = true
        )
        Spacer(Modifier.height(Dimen.Space8))
    }
}
