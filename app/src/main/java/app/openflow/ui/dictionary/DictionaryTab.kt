package app.openflow.ui.dictionary

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
import app.openflow.ui.dictionary.PairImportBlock

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryTab(app: OpenFlowApp) {
    val words by app.dictations.observeDictionary().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var dictSort by rememberSaveable { mutableStateOf(DictListPolicy.Sort.ALPHA.name) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var word by rememberSaveable { mutableStateOf("") }
    var repl by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val sort = DictListPolicy.fromPref(dictSort)
    val shown = remember(words, query, sort) {
        val filtered = words.filter { HubListPolicy.matches(query, it.word, it.replacement) }
        DictListPolicy.apply(filtered, sort, { it.createdAtEpochMs }, { it.word })
    }

    Box(Modifier.fillMaxSize().background(SecUi.cream)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item(key = "dict-search") {
                OpenTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search dictionary…",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SecUi.muted
                        )
                    },
                    modifier = Modifier.testTag("dict_search")
                )
            }

            item(key = "dict-sort") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    modifier = Modifier.testTag("dict_sort")
                ) {
                    OpenChip(
                        label = "A–Z",
                        isOn = sort == DictListPolicy.Sort.ALPHA,
                        modifier = Modifier.testTag("dict_sort_alpha"),
                        onClick = { dictSort = DictListPolicy.Sort.ALPHA.name }
                    )
                    OpenChip(
                        label = "Newest",
                        isOn = sort == DictListPolicy.Sort.NEWEST,
                        modifier = Modifier.testTag("dict_sort_newest"),
                        onClick = { dictSort = DictListPolicy.Sort.NEWEST.name }
                    )
                    OpenChip(
                        label = "Oldest",
                        isOn = sort == DictListPolicy.Sort.OLDEST,
                        modifier = Modifier.testTag("dict_sort_oldest"),
                        onClick = { dictSort = DictListPolicy.Sort.OLDEST.name }
                    )
                }
            }

            if (shown.isEmpty()) {
                item(key = "dict-empty") {
                    EmptyState(
                        icon = Icons.Default.Tune,
                        title = if (query.isBlank()) "No Dict words" else "No matching results",
                        subtitle = if (query.isBlank()) {
                            "Tap + to add a heard word and what to insert."
                        } else {
                            "Try a different search keyword."
                        },
                        modifier = Modifier.testTag("dict_empty")
                    )
                }
            } else {
                if (query.isBlank()) {
                    item(key = "dict-clear") {
                        OpenButton(
                            text = "Clear all learned",
                            modifier = Modifier.testTag("dict_clear_learned"),
                            onClick = {
                                scope.launch { app.dictations.clearLearned() }
                            }
                        )
                    }
                }
                val autoSet = LearnEngine.autoKeys()
                val autoShown = shown.filter { it.word.lowercase() in autoSet }
                val rest = shown.filter { it.word.lowercase() !in autoSet }
                if (autoShown.isNotEmpty()) {
                    item(key = "dict-auto-header") {
                        Text(
                            "Learned (auto)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                        )
                    }
                    items(
                        items = autoShown,
                        key = { "auto-" + UiScrollPolicy.dictRowKey(it.id) },
                        contentType = { "dict-auto" },
                    ) { w: DictionaryWordEntity ->
                        OpenCard(modifier = Modifier.testTag("dict_auto_row")) {
                            Row(
                                Modifier
                                    .padding(Dimen.MIN_PADDING)
                                    .fillMaxWidth()
                                    .heightIn(min = Dimen.MIN_TOUCH),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        w.word,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SecUi.charcoal,
                                        softWrap = true
                                    )
                                    Text(
                                        "→ ${w.replacement}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecUi.ink,
                                        softWrap = true
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { app.dictations.forget(w.word) }
                                    },
                                    modifier = Modifier
                                        .size(Dimen.MIN_TOUCH)
                                        .testTag("dict_auto_forget")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SecUi.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                items(
                    items = rest,
                    key = { UiScrollPolicy.dictRowKey(it.id) },
                    contentType = { "dict" },
                ) { w: DictionaryWordEntity ->
                    OpenCard {
                        Row(
                            Modifier
                                .padding(Dimen.MIN_PADDING)
                                .fillMaxWidth()
                                .heightIn(min = Dimen.MIN_TOUCH),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    w.word,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SecUi.charcoal,
                                    softWrap = true
                                )
                                Text(
                                    "→ ${w.replacement}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecUi.ink,
                                    softWrap = true
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch { app.dictations.deleteWord(w.id) }
                                },
                                modifier = Modifier
                                    .size(Dimen.MIN_TOUCH)
                                    .testTag("dict_delete")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = SecUi.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dict_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add dictionary word")
        }
        if (showAdd) {
            Dialog(onDismissRequest = { showAdd = false }) {
                OpenCard {
                    Column(
                        Modifier
                            .padding(Dimen.MIN_PADDING)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Add to Dict",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        OpenTextField(
                            value = word,
                            onValueChange = { word = it },
                            label = "Heard word",
                            placeholder = "Heard word / mistake (e.g. Wisper)",
                            modifier = Modifier.testTag("dict_word")
                        )
                        OpenTextField(
                            value = repl,
                            onValueChange = { repl = it },
                            label = "Replace with",
                            placeholder = "Replace with (e.g. Wispr)",
                            modifier = Modifier.testTag("dict_repl")
                        )
                        OpenButton(
                            text = "Save word",
                            modifier = Modifier.testTag("dict_save_word"),
                            enabled = word.isNotBlank(),
                            onClick = {
                                if (word.isNotBlank()) {
                                    scope.launch {
                                        val ok = app.dictations.addWord(
                                            word.trim(),
                                            repl.ifBlank { word }.trim()
                                        )
                                        if (!ok) {
                                            Toast.makeText(
                                                ctx,
                                                "That word is a snippet trigger",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            word = ""
                                            repl = ""
                                            showAdd = false
                                        }
                                    }
                                }
                            }
                        )
                        PairImportBlock(
                            testPrefix = "dict",
                            onImport = { app.dictations.importDictionary(it) }
                        )
                        OpenButton(
                            text = "Close",
                            variant = ButtonVariant.Outlined,
                            onClick = { showAdd = false },
                            modifier = Modifier.testTag("dict_add_close")
                        )
                    }
                }
            }
        }
    }
}
