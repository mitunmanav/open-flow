package app.openflow.ui.history

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.TimeZone
import app.openflow.OpenFlowApp
import app.openflow.export.ExportChoice
import app.openflow.export.ExportFormat
import app.openflow.export.HistoryExport
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.history.DictationCard
import app.openflow.ui.home.HistoryDays
import app.openflow.ui.home.HistorySearchPolicy
import app.openflow.ui.home.UiScrollPolicy
import app.openflow.ui.theme.SecUi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
        contentPadding = PaddingValues(bottom = 88.dp)
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
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface
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
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface
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
