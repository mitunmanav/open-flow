package app.openflow.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.openflow.OpenFlowApp
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.data.DictationEntity
import app.openflow.data.DictionaryWordEntity
import app.openflow.data.SnippetEntity
import app.openflow.export.HistoryExport
import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.LayoutPrefs
import app.openflow.text.TextPostProcessor
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.theme.OpenFlowColors
import app.openflow.ui.theme.OpenFlowTheme
import app.openflow.ui.theme.VisualSkin
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OpenFlowApp
        setContent {
            val darkMode by app.prefs.darkMode.collectAsState()
            val skin by app.prefs.visualSkin.collectAsState()
            OpenFlowTheme(darkMode = darkMode, skin = skin) {
                var route by remember { mutableStateOf(AppRoute.Home) }
                var bubbleOn by remember { mutableStateOf(FlowAccessibilityService.isRunning()) }
                var micOn by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val owner = LocalLifecycleOwner.current
                DisposableEffect(owner) {
                    val obs = LifecycleEventObserver { _, e ->
                        if (e == Lifecycle.Event.ON_RESUME) {
                            bubbleOn = FlowAccessibilityService.isRunning()
                            micOn = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            FlowAccessibilityService.instance?.applyPrefsVisual()
                        }
                    }
                    owner.lifecycle.addObserver(obs)
                    onDispose { owner.lifecycle.removeObserver(obs) }
                }

                var layoutTick by remember { mutableIntStateOf(0) }
                AppShell(
                    route = route,
                    onNavigate = { dest -> route = dest },
                    isDrawerExtraVisible = { true }
                ) { padding ->
                    AnimatedContent(
                        targetState = route to layoutTick,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        },
                        label = "route_content",
                        modifier = Modifier.padding(padding)
                    ) { (r, _) ->
                        when (r) {
                            AppRoute.Home -> HomeHub(
                                app = app,
                                bubbleOn = bubbleOn,
                                micOn = micOn,
                                onEnableBubble = {
                                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                                onOpenHistory = { route = AppRoute.History },
                                onOpenBubbleSettings = { route = AppRoute.BubbleSettings },
                                onOpenAppearance = { route = AppRoute.Appearance },
                                onOpenCleanup = { route = AppRoute.Cleanup },
                                onOpenStyle = { route = AppRoute.Style }
                            )
                            AppRoute.History -> HistoryScreen(app)
                            AppRoute.Dictionary -> DictionaryTab(app)
                            AppRoute.Snippets -> SnippetsTab(app)
                            AppRoute.Style -> StyleTab(app.prefs)
                            AppRoute.Settings -> SettingsHub(
                                onDictionary = { route = AppRoute.Dictionary },
                                onSnippets = { route = AppRoute.Snippets },
                                onStyle = { route = AppRoute.Style },
                                onAppearance = { route = AppRoute.Appearance },
                                onBubble = { route = AppRoute.BubbleSettings },
                                onCleanup = { route = AppRoute.Cleanup },
                                onPrivacy = { route = AppRoute.Privacy },
                                onSounds = { route = AppRoute.Sounds },
                                onHomeLayout = { route = AppRoute.HomeModules },
                                onNavLayout = { route = AppRoute.NavModules }
                            )
                            AppRoute.Customize -> CustomizeHub(
                                onHomeLayout = { route = AppRoute.HomeModules },
                                onNavLayout = { route = AppRoute.NavModules }
                            )
                            AppRoute.Appearance -> AppearanceSettings(app.prefs)
                            AppRoute.BubbleSettings -> BubbleSettings(
                                prefs = app.prefs,
                                onApplyBubble = {
                                    FlowAccessibilityService.instance?.applyPrefsVisual()
                                }
                            )
                            AppRoute.Cleanup -> CleanupSettings(app.prefs)
                            AppRoute.Privacy -> PrivacySettings(app.prefs)
                            AppRoute.Sounds -> SoundsSettings(app.prefs)
                            AppRoute.HomeModules -> ModuleEditor(
                                title = "Home layout",
                                subtitle = "Show, hide, reorder Home cards",
                                modules = app.prefs.homeModules(),
                                labels = mapOf(
                                    "setup" to "Setup",
                                    "stats" to "Stats",
                                    "keys" to "Key actions",
                                    "test" to "Test field",
                                    "recent" to "Recent history"
                                ),
                                onChange = {
                                    app.prefs.setHomeModules(it)
                                    layoutTick++
                                }
                            )
                            AppRoute.NavModules -> ModuleEditor(
                                title = "Drawer extras",
                                subtitle = "Settings always stays. Bottom tabs are not listed here.",
                                modules = app.prefs.navModules(),
                                labels = mapOf(
                                    "history" to "History",
                                    "customize" to "Customize"
                                ),
                                onChange = {
                                    app.prefs.setNavModules(it)
                                    layoutTick++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHub(
    app: OpenFlowApp,
    bubbleOn: Boolean,
    micOn: Boolean,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBubbleSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenCleanup: () -> Unit,
    onOpenStyle: () -> Unit
) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var statsText by remember { mutableStateOf("…") }
    var localNote by remember { mutableStateOf("") }
    var cleanup by remember { mutableStateOf(app.prefs.cleanupLevel) }
    var lastClean by remember { mutableStateOf(app.prefs.lastCleanText) }
    var lastRaw by remember { mutableStateOf(app.prefs.lastRawText) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                lastClean = app.prefs.lastCleanText
                lastRaw = app.prefs.lastRawText
            }
        }
        owner.lifecycle.addObserver(obs)
        scope.launch {
            val s = app.dictations.stats()
            statsText = "${s.totalWords} words spoken · ${s.totalSessions} sessions · ${s.streakDays}d streak"
        }
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val ready = bubbleOn && micOn

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Brand Status Card
        OpenCard {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (ready) "Flow Ready" else "Setup Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Wispr-grade On-Device Voice Flow",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = if (ready) OpenFlowColors.SuccessContainer else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (ready) "ACTIVE" else "OFFLINE",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (ready) OpenFlowColors.Success else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    if (ready) {
                        "Open any app → tap the floating Flow Bubble → speak → tap stop. Clean text is automatically inserted."
                    } else {
                        "Enable the Accessibility Service and Microphone permission to activate the floating bubble."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpenChip(
                        label = if (bubbleOn) "Bubble Active" else "Bubble Disabled",
                        isOn = bubbleOn,
                        showCheckWhenOn = true,
                        onClick = onEnableBubble
                    )
                    OpenChip(
                        label = if (micOn) "Mic Allowed" else "Mic Denied",
                        isOn = micOn,
                        showCheckWhenOn = true,
                        onClick = onMic
                    )
                }

                if (!bubbleOn) {
                    OpenButton(
                        text = "Enable Floating Bubble",
                        onClick = onEnableBubble
                    )
                }
                if (!micOn) {
                    OpenButton(
                        text = "Allow Microphone",
                        onClick = onMic,
                        variant = if (bubbleOn) ButtonVariant.Filled else ButtonVariant.Outlined
                    )
                }
                if (ready) {
                    OpenButton(
                        text = "Customize Floating Bubble",
                        onClick = onOpenBubbleSettings,
                        variant = ButtonVariant.Outlined
                    )
                }
            }
        }

        // Practice Field
        OpenCard {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Live Playground",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (localNote.isNotBlank()) {
                        Text(
                            "${localNote.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "Focus this box, then tap the floating bubble to test dictation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OpenTextField(
                    value = localNote,
                    onValueChange = { localNote = it },
                    placeholder = "Tap floating bubble · speak · tap stop…",
                    minLines = 3
                )
            }
        }

        // Quick Tuning Bar
        OpenCard {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cleanup Polish",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "English (en-US)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "none" to "Raw",
                        "light" to "Smart",
                        "medium" to "Normal",
                        "high" to "Formal"
                    ).forEach { (level, label) ->
                        OpenChip(
                            label = label,
                            isOn = cleanup == level,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                cleanup = level
                                app.prefs.cleanupLevel = level
                            }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onOpenCleanup) { Text("Cleanup rules") }
                    TextButton(onClick = onOpenStyle) { Text("Voice style") }
                    TextButton(onClick = onOpenAppearance) { Text("Theme") }
                }
            }
        }

        // Last Result Card
        if (lastClean.isNotBlank()) {
            OpenCard {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Latest Dictation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Last session",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        lastClean.take(600),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CopyButton(text = lastClean, label = "Copy Clean")
                        if (lastRaw.isNotBlank() && lastRaw != lastClean) {
                            CopyButton(text = lastRaw, label = "Copy Raw")
                        }
                    }
                }
            }
        }

        // Recent Dictations
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    statsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenHistory) { Text("View all (${dictations.size})") }
        }

        if (dictations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = "No dictations yet",
                subtitle = "Dictate with the floating bubble anywhere to build your private memory."
            )
        } else {
            dictations.take(4).forEach { d: DictationEntity ->
                DictationCard(
                    d = d,
                    onDelete = {
                        scope.launch { app.dictations.deleteDictation(d.id) }
                    },
                    onShare = {
                        val rows = listOf(HistoryExport.Row(d.createdAtEpochMs, d.text, d.languageTag, d.wordCount))
                        val shareText = HistoryExport.shareText(rows)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Share dictation"))
                    }
                )
            }
        }

        Text(
            "Speech recognition runs locally via Android SpeechRecognizer. Open Flow never sends your audio to the cloud.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HistoryScreen(app: OpenFlowApp) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val filtered = remember(dictations, searchQuery) {
        if (searchQuery.isBlank()) dictations
        else dictations.filter {
            it.text.contains(searchQuery, ignoreCase = true) ||
                it.rawText.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Private History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${dictations.size} recordings on device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dictations.isNotEmpty()) {
                OpenButton(
                    text = "Export",
                    onClick = {
                        val rows = dictations.map { d ->
                            HistoryExport.Row(d.createdAtEpochMs, d.text, d.languageTag, d.wordCount)
                        }
                        val shareText = HistoryExport.toMarkdown(rows)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Export history (Markdown)"))
                    },
                    variant = ButtonVariant.Outlined
                )
            }
        }

        OpenTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search transcripts…",
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = if (searchQuery.isBlank()) "No history yet" else "No matching results",
                subtitle = if (searchQuery.isBlank()) "Dictate using the floating bubble to record transcripts." else "Try a different search keyword."
            )
        } else {
            filtered.forEach { d ->
                DictationCard(
                    d = d,
                    onDelete = {
                        scope.launch { app.dictations.deleteDictation(d.id) }
                    },
                    onShare = {
                        val rows = listOf(HistoryExport.Row(d.createdAtEpochMs, d.text, d.languageTag, d.wordCount))
                        val shareText = HistoryExport.shareText(rows)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Share dictation"))
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DictationCard(d: DictationEntity, onDelete: () -> Unit, onShare: () -> Unit) {
    var showRaw by remember { mutableStateOf(false) }
    val hasRaw = d.rawText.isNotBlank() && d.rawText != d.text
    val timeStr = remember(d.createdAtEpochMs) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(d.createdAtEpochMs))
    }

    OpenCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$timeStr · ${d.wordCount}w",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                d.text.take(500),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (hasRaw) {
                TextButton(
                    onClick = { showRaw = !showRaw },
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        if (showRaw) "Hide Raw STT" else "Show Raw STT",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (showRaw) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            d.rawText.take(400),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyButton(text = d.text, label = "Copy")
                if (hasRaw) {
                    CopyButton(text = d.rawText, label = "Copy Raw")
                }
            }
        }
    }
}

@Composable
private fun CopyButton(text: String, label: String = "Copy") {
    val ctx = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("dictation", text))
            copied = true
        }
    ) {
        Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (copied) OpenFlowColors.Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(if (copied) "Copied!" else label)
    }
}

@Composable
private fun DictionaryTab(app: OpenFlowApp) {
    val words by app.dictations.observeDictionary().collectAsState(initial = emptyList())
    var word by remember { mutableStateOf("") }
    var repl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Custom Vocabulary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Local replacement rules applied instantly during insertion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Replacement Rule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OpenTextField(
                    value = word,
                    onValueChange = { word = it },
                    placeholder = "Heard word / mistake (e.g. Wisper)"
                )
                OpenTextField(
                    value = repl,
                    onValueChange = { repl = it },
                    placeholder = "Replace with (e.g. Wispr)"
                )
                OpenButton(
                    text = "Save Word",
                    onClick = {
                        if (word.isNotBlank()) {
                            scope.launch {
                                app.dictations.addWord(word.trim(), repl.ifBlank { word }.trim())
                                word = ""
                                repl = ""
                            }
                        }
                    }
                )
            }
        }

        if (words.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Tune,
                title = "No vocabulary rules",
                subtitle = "Add unusual names, acronyms, or tech jargon to ensure correct spelling."
            )
        } else {
            words.forEach { w: DictionaryWordEntity ->
                OpenCard {
                    Row(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(w.word, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "→ ${w.replacement}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            scope.launch { app.dictations.deleteWord(w.id) }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SnippetsTab(app: OpenFlowApp) {
    val snippets by app.dictations.observeSnippets().collectAsState(initial = emptyList())
    var trigger by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Voice Snippets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Speak the trigger phrase alone to automatically expand into full text.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("New Voice Snippet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OpenTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    placeholder = "Trigger (e.g. my address, email sig)"
                )
                OpenTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = "Expansion text…",
                    minLines = 3
                )
                OpenButton(
                    text = "Add Snippet",
                    onClick = {
                        if (trigger.isNotBlank() && body.isNotBlank()) {
                            scope.launch {
                                app.dictations.addSnippet(trigger.trim(), body.trim())
                                trigger = ""
                                body = ""
                            }
                        }
                    }
                )
            }
        }

        if (snippets.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Tune,
                title = "No voice snippets",
                subtitle = "Create shortcuts for frequently typed addresses, emails, or templates."
            )
        } else {
            snippets.forEach { s: SnippetEntity ->
                OpenCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Trigger: \"${s.trigger}\"",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { scope.launch { app.dictations.deleteSnippet(s.id) } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            s.body.take(200),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StyleTab(prefs: FlowPrefs) {
    val styles = TextPostProcessor.Style.entries
    var selected by remember { mutableStateOf(prefs.style()) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Writing Style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Choose the default tone for post-processing voice transcripts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        styles.forEach { st ->
            val on = selected == st
            OpenCard(
                selected = on,
                onClick = {
                    selected = st
                    prefs.styleName = st.name
                }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            st.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when (st) {
                                TextPostProcessor.Style.CASUAL -> "Natural, everyday spoken tone with clean punctuation."
                                TextPostProcessor.Style.FORMAL -> "Professional, polished phrasing suitable for emails & docs."
                                TextPostProcessor.Style.EXCITED -> "High energy with exclamation accents."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsHub(
    onDictionary: () -> Unit,
    onSnippets: () -> Unit,
    onStyle: () -> Unit,
    onAppearance: () -> Unit,
    onBubble: () -> Unit,
    onCleanup: () -> Unit,
    onPrivacy: () -> Unit,
    onSounds: () -> Unit,
    onHomeLayout: () -> Unit,
    onNavLayout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Preferences & local configuration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsRow("Flow Bubble & Gestures", "Shape, size, opacity, edge magnetic snap", onBubble)
        SettingsRow("Cleanup Pipeline", "Filler words, course corrections, lists", onCleanup)
        SettingsRow("Writing Style", "Casual, formal, concise persona", onStyle)
        SettingsRow("Custom Vocabulary", "Personalized spelling & acronyms", onDictionary)
        SettingsRow("Voice Snippets", "Trigger phrases → text expansion", onSnippets)
        SettingsRow("Appearance", "Dark / light theme, visual skins", onAppearance)
        SettingsRow("Privacy & Retention", "Zero-cloud audit, auto-wipe policies", onPrivacy)
        SettingsRow("Haptics & Feedback", "Tactile clicks and audio feedback", onSounds)
        SettingsRow("Home Layout", "Reorder and toggle Home cards", onHomeLayout)
        SettingsRow("Drawer Extras", "Customize navigation drawer", onNavLayout)

        Text(
            "Open Flow is 100% Free & Open Source (MIT License). Zero trackers. Zero analytics.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CustomizeHub(
    onHomeLayout: () -> Unit,
    onNavLayout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Customize", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingsRow("Home layout", "Modules on Home", onHomeLayout)
        SettingsRow("Drawer extras", "What appears in the menu", onNavLayout)
    }
}

@Composable
private fun CleanupSettings(prefs: FlowPrefs) {
    var level by remember { mutableStateOf(prefs.cleanupLevel) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Cleanup Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Real-time local text cleanup applied before inserting into fields.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        listOf(
            "none" to ("Raw STT" to "Exact words without any filtering or post-processing."),
            "light" to ("Smart Filter" to "Strips filler words (um, uh, like) and fixes basic punctuation."),
            "medium" to ("Normal" to "Smart filter + course corrections ('430 actually 530') + punctuation commands."),
            "high" to ("High Polish" to "Normal + numbered lists auto-formatting and strong syntax cleanup.")
        ).forEach { (v, pair) ->
            val (title, desc) = pair
            val on = level == v
            OpenCard(
                selected = on,
                onClick = {
                    level = v
                    prefs.cleanupLevel = v
                }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacySettings(prefs: FlowPrefs) {
    var ret by remember { mutableStateOf(prefs.retentionPolicy) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Privacy & Storage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "All transcripts and settings remain strictly on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        listOf(
            "keep" to ("Keep Forever" to "Store private dictation history in encrypted on-device SQLite database."),
            "wipe_24h" to ("Wipe After 24h" to "Automatically clear dictation history older than 24 hours."),
            "never_store" to ("Incognito Mode" to "Never write transcripts to local storage. Only insert directly.")
        ).forEach { (v, pair) ->
            val (title, desc) = pair
            val on = ret == v
            OpenCard(
                selected = on,
                onClick = {
                    ret = v
                    prefs.retentionPolicy = v
                }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SoundsSettings(prefs: FlowPrefs) {
    var sounds by remember { mutableStateOf(prefs.bubbleSounds) }
    var haptics by remember { mutableStateOf(prefs.bubbleHaptics) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Feedback & Cues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Haptic and audio cues during dictation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        OpenCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Start / Stop Audio Cue", style = MaterialTheme.typography.titleSmall)
                    Text("Play subtle tone when starting dictation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OpenChip(
                    label = if (sounds) "ON" else "OFF",
                    isOn = sounds,
                    onClick = {
                        sounds = !sounds
                        prefs.bubbleSounds = sounds
                    }
                )
            }
        }

        OpenCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tactile Haptics", style = MaterialTheme.typography.titleSmall)
                    Text("Vibrate on tap, PTT hold, and edge snap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OpenChip(
                    label = if (haptics) "ON" else "OFF",
                    isOn = haptics,
                    onClick = {
                        haptics = !haptics
                        prefs.bubbleHaptics = haptics
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModuleEditor(
    title: String,
    subtitle: String,
    modules: List<LayoutPrefs.Module>,
    labels: Map<String, String>,
    lockVisible: Set<String> = emptySet(),
    onChange: (List<LayoutPrefs.Module>) -> Unit
) {
    var local by remember(modules) { mutableStateOf(modules) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        local.forEach { m ->
            val locked = m.id in lockVisible
            OpenCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        labels[m.id] ?: m.id,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OpenChip(
                            label = if (m.visible) "Visible" else "Hidden",
                            isOn = m.visible,
                            onClick = {
                                if (!locked) {
                                    local = LayoutPrefs.toggleVisible(local, m.id)
                                    onChange(local)
                                }
                            }
                        )
                        TextButton(onClick = {
                            local = LayoutPrefs.move(local, m.id, -1)
                            onChange(local)
                        }) { Text("Move Up") }
                        TextButton(onClick = {
                            local = LayoutPrefs.move(local, m.id, 1)
                            onChange(local)
                        }) { Text("Move Down") }
                    }
                    if (locked) {
                        Text(
                            "Always visible by default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    OpenCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppearanceSettings(prefs: FlowPrefs) {
    val dark by prefs.darkMode.collectAsState()
    val skin by prefs.visualSkin.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Customize theme colors and component styling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Color Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (v, label) ->
                        OpenChip(
                            label = label,
                            isOn = dark == v,
                            modifier = Modifier.weight(1f),
                            onClick = { prefs.setDarkMode(v) }
                        )
                    }
                }
            }
        }

        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Design Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpenChip(
                        label = "M3 Soft",
                        isOn = skin == VisualSkin.M3,
                        modifier = Modifier.weight(1f),
                        onClick = { prefs.setVisualSkin(VisualSkin.M3) }
                    )
                    OpenChip(
                        label = "Modern Brutal",
                        isOn = skin == VisualSkin.BRUTAL,
                        modifier = Modifier.weight(1f),
                        onClick = { prefs.setVisualSkin(VisualSkin.BRUTAL) }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BubbleSettings(prefs: FlowPrefs, onApplyBubble: () -> Unit) {
    var scale by remember { mutableFloatStateOf(prefs.bubbleScale) }
    var opacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var shape by remember { mutableStateOf(prefs.bubbleShape) }
    var showText by remember { mutableStateOf(prefs.bubbleShowText) }
    var snap by remember { mutableStateOf(prefs.bubbleEdgeSnap) }
    var haptics by remember { mutableStateOf(prefs.bubbleHaptics) }
    var pulse by remember { mutableStateOf(prefs.bubblePulse) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Flow Bubble Customizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Morph the shape, size, and interaction physics of your floating bubble.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Shape Picker
        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Overlay Shape", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "pill" to "Pill",
                        "circle" to "Circle",
                        "square" to "Squircle",
                        "dot" to "Dot"
                    ).forEach { (v, label) ->
                        OpenChip(
                            label = label,
                            isOn = shape == v,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                shape = v
                                prefs.bubbleShape = v
                                onApplyBubble()
                            }
                        )
                    }
                }
            }
        }

        // Size & Opacity
        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dimensions & Transparency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scale", style = MaterialTheme.typography.bodyMedium)
                    Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                        prefs.bubbleScale = it
                        onApplyBubble()
                    },
                    valueRange = 0.7f..1.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opacity", style = MaterialTheme.typography.bodyMedium)
                    Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        prefs.bubbleOpacity = it
                        onApplyBubble()
                    },
                    valueRange = 0.3f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Toggles
        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Interactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Live Speech Caption", style = MaterialTheme.typography.bodyMedium)
                        Text("Display transcribed words directly on the bubble", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OpenChip(
                        label = if (showText) "ON" else "OFF",
                        isOn = showText,
                        onClick = {
                            showText = !showText
                            prefs.bubbleShowText = showText
                            onApplyBubble()
                        }
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Magnetic Edge Snapping", style = MaterialTheme.typography.bodyMedium)
                        Text("Snap bubble seamlessly to nearest screen edge on release", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OpenChip(
                        label = if (snap) "ON" else "OFF",
                        isOn = snap,
                        onClick = {
                            snap = !snap
                            prefs.bubbleEdgeSnap = snap
                            onApplyBubble()
                        }
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Active Recording Pulse", style = MaterialTheme.typography.bodyMedium)
                        Text("Pulse glowing outer ring and scale dynamically to voice RMS volume", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OpenChip(
                        label = if (pulse) "ON" else "OFF",
                        isOn = pulse,
                        onClick = {
                            pulse = !pulse
                            prefs.bubblePulse = pulse
                            onApplyBubble()
                        }
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tactile Haptics", style = MaterialTheme.typography.bodyMedium)
                        Text("Vibration feedback on tap and long-press push-to-talk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OpenChip(
                        label = if (haptics) "ON" else "OFF",
                        isOn = haptics,
                        onClick = {
                            haptics = !haptics
                            prefs.bubbleHaptics = haptics
                            onApplyBubble()
                        }
                    )
                }
            }
        }

        OpenButton(
            text = "Wake / Reset Bubble",
            onClick = {
                prefs.clearSnooze()
                onApplyBubble()
            },
            variant = ButtonVariant.Outlined
        )
        Spacer(Modifier.height(24.dp))
    }
}
