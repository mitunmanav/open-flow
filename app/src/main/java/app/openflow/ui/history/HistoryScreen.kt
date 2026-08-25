package app.openflow.ui.history

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.openflow.R
import app.openflow.OpenFlowApp
import app.openflow.bubble.BubbleChrome
import app.openflow.bubble.BubbleIconPolicy
import app.openflow.bubble.BubbleScaleSteps
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.data.DictationEntity
import app.openflow.data.DictionaryWordEntity
import app.openflow.data.ProcessStatus
import app.openflow.data.SnippetEntity
import app.openflow.export.ExportChoice
import app.openflow.export.ExportFormat
import app.openflow.export.HistoryExport
import app.openflow.help.HelpLinks
import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.LayoutPrefs
import app.openflow.text.LearnEngine
import app.openflow.text.PairImport
import app.openflow.text.WritingStyle
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.display.DisplayRefreshController
import app.openflow.display.DisplayRefreshPolicy
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.OnDeviceSpeechPolicy
import app.openflow.stt.SttTuning
import app.openflow.ui.engine.EngineSettingsScreen
import app.openflow.ui.home.DictListPolicy
import app.openflow.ui.home.HubListPolicy
import app.openflow.ui.home.HistoryDays
import app.openflow.ui.home.HistorySearchPolicy
import app.openflow.ui.home.HistoryRowActions
import app.openflow.ui.home.HomeBannerPolicy
import app.openflow.ui.haptics.HapticsSettings
import app.openflow.ui.history.DictationCard
import app.openflow.ui.home.HomeFeed
import app.openflow.ui.home.ModuleEditorVisibility
import app.openflow.ui.home.UiScrollPolicy
import app.openflow.ui.insights.InsightsScreen
import app.openflow.ui.legal.LegalCopy
import app.openflow.ui.legal.LegalDocumentScreen
import app.openflow.ui.privacy.PrivacyHonesty
import app.openflow.ui.setup.BatteryExemption
import app.openflow.ui.setup.FirstRunPolicy
import app.openflow.ui.setup.SetupWizard
import app.openflow.ui.style.StyleHubScreen
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.shell.NavStack
import app.openflow.ui.UiHapticMap
import app.openflow.ui.theme.BubbleTint
import app.openflow.ui.theme.HexColor
import app.openflow.ui.theme.Motion
import app.openflow.ui.theme.OpenFlowTheme
import app.openflow.ui.theme.rememberMotionMs
import app.openflow.ui.theme.rememberShouldAnimate
import app.openflow.ui.walkthrough.WalkthroughPager
import app.openflow.ui.walkthrough.WalkthroughPolicy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import app.openflow.ui.theme.SecUi
import app.openflow.ui.history.DictationCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(app: OpenFlowApp) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var exportFormatName by rememberSaveable { mutableStateOf(ExportFormat.MARKDOWN.name) }
    var exportRaw by rememberSaveable { mutableStateOf(false) }
    val pendingExport = remember { arrayOf("") }
    val saveDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val body = pendingExport[0]
        if (uri == null || body.isEmpty()) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
            } ?: error("no stream")
        } catch (_: Exception) {
            Toast.makeText(ctx, "Could not save", Toast.LENGTH_SHORT).show()
        }
    }
    val match = HistorySearchPolicy.ftsMatch(searchQuery)
    // Lint false positive (compose-bom 2024.10): value IS assigned unconditionally.
    @SuppressLint("ProduceStateDoesNotAssignValue")
    val filtered by produceState(initialValue = dictations, match, dictations) {
        val next = if (match == null) {
            dictations
        } else {
            app.dictations.searchDictations(searchQuery)
        }
        value = next
    }
    val days = remember(filtered) {
        val nowMs = System.currentTimeMillis()
        HistoryDays.group(
            filtered.map { HistoryDays.Row(it.id, it.createdAtEpochMs, it.text) },
            nowMs = nowMs,
            zoneOffsetMs = TimeZone.getDefault().getOffset(nowMs).toLong()
        )
    }
    val byId = remember(filtered) { filtered.associateBy { it.id } }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
        contentPadding = PaddingValues(bottom = Dimen.GAP_LG)
    ) {
        item(key = "history-hdr") {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            Text(
                "${dictations.size} recordings on device",
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            if (dictations.isNotEmpty()) {
                val fmt = runCatching { ExportFormat.valueOf(exportFormatName) }
                    .getOrDefault(ExportFormat.MARKDOWN)
                val rows = dictations.map { d ->
                    HistoryExport.Row(
                        d.createdAtEpochMs,
                        d.text,
                        d.languageTag,
                        d.wordCount,
                        d.rawText,
                        d.id,
                        d.durationMs,
                    )
                }
                val body = HistoryExport.render(rows, ExportChoice(fmt, exportRaw))
                Column(verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                    ) {
                        OpenChip(
                            label = "MD",
                            isOn = fmt == ExportFormat.MARKDOWN,
                            modifier = Modifier.testTag("history_export_md"),
                            onClick = { exportFormatName = ExportFormat.MARKDOWN.name }
                        )
                        OpenChip(
                            label = "Plain",
                            isOn = fmt == ExportFormat.PLAIN,
                            modifier = Modifier.testTag("history_export_plain"),
                            onClick = { exportFormatName = ExportFormat.PLAIN.name }
                        )
                        OpenChip(
                            label = "JSON",
                            isOn = fmt == ExportFormat.JSON,
                            modifier = Modifier.testTag("history_export_json"),
                            onClick = { exportFormatName = ExportFormat.JSON.name }
                        )
                        OpenChip(
                            label = "Raw",
                            isOn = exportRaw,
                            modifier = Modifier.testTag("history_export_raw"),
                            onClick = { exportRaw = !exportRaw }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                                try {
                                    ctx.startActivity(Intent.createChooser(send, "Export history"))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = Dimen.MIN_TOUCH)
                                .testTag("history_export"),
                            shape = MaterialTheme.shapes.small,
                            border = SecUi.hardBorder,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SecUi.charcoal,
                                containerColor = SecUi.cream
                            )
                        ) {
                            Text("Share", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                pendingExport[0] = body
                                val name = when (fmt) {
                                    ExportFormat.MARKDOWN -> "open-flow-history.md"
                                    ExportFormat.PLAIN -> "open-flow-history.txt"
                                    ExportFormat.JSON -> "open-flow-history.json"
                                }
                                try {
                                    saveDoc.launch(name)
                                } catch (_: Exception) {
                                    Toast.makeText(ctx, "Could not save", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = Dimen.MIN_TOUCH)
                                .testTag("history_export_save"),
                            shape = MaterialTheme.shapes.small,
                            border = SecUi.hardBorder,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SecUi.charcoal,
                                containerColor = SecUi.cream
                            )
                        ) {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        }

        item(key = "history-search") {
        OpenTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search transcripts…",
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SecUi.muted
                )
            }
        )
        }

        if (filtered.isEmpty()) {
            item(key = "history-empty") {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = if (searchQuery.isBlank()) "No history yet" else "No matching results",
                subtitle = if (searchQuery.isBlank()) {
                    "Dictate using the floating bubble to record transcripts."
                } else {
                    "Try a different search keyword."
                },
                modifier = Modifier.testTag("history_empty")
            )
            }
        } else {
            days.forEach { day ->
                val firstId = day.rows.firstOrNull()?.id.orEmpty()
                item(key = UiScrollPolicy.dayHeaderKey(day.label, firstId)) {
                Text(
                    day.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                }
                items(
                    items = day.rows,
                    key = { UiScrollPolicy.historyRowKey(it.id) },
                    contentType = { "hist" },
                ) { row ->
                    val d = byId[row.id] ?: return@items
                    DictationCard(
                        d = d,
                        onDelete = {
                            scope.launch { app.dictations.deleteDictation(d.id) }
                        },
                        onShare = {
                            val rows = listOf(
                                HistoryExport.Row(
                                    d.createdAtEpochMs,
                                    d.text,
                                    d.languageTag,
                                    d.wordCount,
                                    d.rawText,
                                )
                            )
                            val shareText = HistoryExport.shareText(rows)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            try {
                                ctx.startActivity(Intent.createChooser(send, "Share dictation"))
                            } catch (_: Exception) {
                            }
                        },
                        onSave = { old, new ->
                            scope.launch {
                                if (app.prefs.autoLearn) {
                                    app.dictations.learnFromEdit(old, new)
                                }
                                app.dictations.updateDictationText(d.id, new)
                            }
                        },
                        onUseRaw = { raw -> useHistoryRaw(ctx, raw) }
                    )
                }
            }
        }
    }
}
