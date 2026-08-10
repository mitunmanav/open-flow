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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.LayoutPrefs
import app.openflow.privacy.PrivacyDefaults
import app.openflow.stt.SttTuning
import app.openflow.text.TextPostProcessor
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.theme.OpenFlowTheme
import app.openflow.ui.theme.VisualSkin
import kotlinx.coroutines.launch

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
            val darkMode by app.prefs.darkMode.collectAsState(initial = app.prefs.darkMode.value)
            val skin by app.prefs.visualSkin.collectAsState(initial = app.prefs.visualSkin.value)
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

                var layoutTick by remember { mutableStateOf(0) }
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
    var statsText by remember { mutableStateOf("…") }
    var localNote by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(app.prefs.languageTag) }
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
            statsText = "${s.totalWords} words · ${s.totalSessions} sessions · ${s.streakDays}d streak"
        }
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val ready = bubbleOn && micOn

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero
        OpenCard {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (ready) "Ready to dictate" else "Finish setup",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (ready) {
                        "Open any app → tap the floating bubble → speak → tap again. Clean text lands in the field."
                    } else {
                        "Turn on the bubble + mic. Keep your normal keyboard."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpenChip(
                        label = if (bubbleOn) "Bubble on" else "Bubble off",
                        isOn = bubbleOn
                    )
                    OpenChip(
                        label = if (micOn) "Mic on" else "Mic off",
                        isOn = micOn
                    )
                }
                if (!bubbleOn) {
                    OpenButton(text = "Enable floating bubble", onClick = onEnableBubble)
                }
                if (!micOn) {
                    OpenButton(text = "Allow microphone", onClick = onMic)
                }
                if (ready) {
                    OpenButton(
                        text = "Bubble & accessibility",
                        onClick = onEnableBubble,
                        variant = ButtonVariant.Outlined
                    )
                }
            }
        }

        // Quick controls
        OpenCard {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Quick controls", style = MaterialTheme.typography.titleMedium)
                Text("Language", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SttTuning.DEFAULT_LANGUAGE to "English US",
                        "en-GB" to "English UK"
                    ).forEach { (tag, label) ->
                        OpenChip(
                            label = label,
                            isOn = lang.equals(tag, ignoreCase = true),
                            onClick = {
                                lang = tag
                                app.prefs.languageTag = tag
                            }
                        )
                    }
                }
                Text("Cleanup", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "none" to "Raw",
                        "light" to "Light",
                        "medium" to "Normal",
                        "high" to "High"
                    ).forEach { (level, label) ->
                        OpenChip(
                            label = label,
                            isOn = cleanup == level,
                            onClick = {
                                cleanup = level
                                app.prefs.cleanupLevel = level
                            }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenBubbleSettings) { Text("Bubble size") }
                    TextButton(onClick = onOpenCleanup) { Text("Cleanup detail") }
                    TextButton(onClick = onOpenStyle) { Text("Style") }
                    TextButton(onClick = onOpenAppearance) { Text("Theme") }
                }
            }
        }

        // Practice field
        OpenCard {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Practice here", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Focus this box, then use the floating bubble.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OpenTextField(
                    value = localNote,
                    onValueChange = { localNote = it },
                    label = "Tap bubble · speak · stop",
                    minLines = 4
                )
            }
        }

        // Last result
        if (lastClean.isNotBlank()) {
            OpenCard {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Last result", style = MaterialTheme.typography.titleMedium)
                    Text(
                        lastClean.take(600),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CopyButton(text = lastClean, label = "Copy")
                        if (lastRaw.isNotBlank() && lastRaw != lastClean) {
                            CopyButton(text = lastRaw, label = "Copy raw")
                        }
                    }
                }
            }
        }

        // Recent
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenHistory) { Text("All history") }
        }
        Text(
            statsText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (dictations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = "Nothing yet",
                subtitle = "Dictate with the bubble, then stop to save."
            )
        } else {
            dictations.take(5).forEach { d: DictationEntity ->
                DictationCard(d, onDelete = {
                    scope.launch { app.dictations.deleteDictation(d.id) }
                })
            }
        }

        Text(
            "Speech uses Android system STT — may leave the device. Open Flow never uploads.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HistoryScreen(app: OpenFlowApp) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("All dictations (on device)", style = MaterialTheme.typography.titleMedium)
        if (dictations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = "Empty",
                subtitle = "Nothing saved yet."
            )
        } else {
            dictations.forEach { d ->
                DictationCard(d, onDelete = {
                    scope.launch { app.dictations.deleteDictation(d.id) }
                })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DictationCard(d: DictationEntity, onDelete: () -> Unit) {
    var showRaw by remember { mutableStateOf(false) }
    val hasRaw = d.rawText.isNotBlank() && d.rawText != d.text
    OpenCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${d.wordCount} words · ${d.languageTag}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(d.text.take(400), style = MaterialTheme.typography.bodyMedium)
            if (hasRaw) {
                TextButton(onClick = { showRaw = !showRaw }) {
                    Text(if (showRaw) "Hide raw" else "Show raw")
                }
                if (showRaw) {
                    Text(
                        d.rawText.take(400),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyButton(text = d.text, label = "Copy cleaned")
                if (hasRaw) {
                    CopyButton(text = d.rawText, label = "Copy raw")
                }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CopyButton(text: String, label: String = "Copy") {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("dictation", text))
        }
    ) { Text(label) }
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
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dictionary", style = MaterialTheme.typography.titleMedium)
        Text("Local replace rules on insert.", style = MaterialTheme.typography.bodySmall)
        OpenTextField(value = word, onValueChange = { word = it }, label = "Word / phrase")
        OpenTextField(value = repl, onValueChange = { repl = it }, label = "Replace with (optional)")
        OpenButton(
            text = "Add",
            onClick = {
                if (word.isNotBlank()) {
                    scope.launch {
                        app.dictations.addWord(word, repl.ifBlank { word })
                        word = ""
                        repl = ""
                    }
                }
            }
        )
        words.forEach { w: DictionaryWordEntity ->
            OpenCard {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${w.word} → ${w.replacement}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = {
                        scope.launch { app.dictations.deleteWord(w.id) }
                    }) { Text("Del") }
                }
            }
        }
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
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Snippets", style = MaterialTheme.typography.titleMedium)
        Text("Say the trigger alone after stop to expand.", style = MaterialTheme.typography.bodySmall)
        OpenTextField(value = trigger, onValueChange = { trigger = it }, label = "Trigger e.g. sig")
        OpenTextField(value = body, onValueChange = { body = it }, label = "Body", minLines = 3)
        OpenButton(
            text = "Add snippet",
            onClick = {
                if (trigger.isNotBlank() && body.isNotBlank()) {
                    scope.launch {
                        app.dictations.addSnippet(trigger, body)
                        trigger = ""
                        body = ""
                    }
                }
            }
        )
        snippets.forEach { s: SnippetEntity ->
            OpenCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.trigger, style = MaterialTheme.typography.titleSmall)
                    Text(s.body.take(200), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        scope.launch { app.dictations.deleteSnippet(s.id) }
                    }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun StyleTab(prefs: FlowPrefs) {
    val styles = TextPostProcessor.Style.entries
    var selected by remember { mutableStateOf(prefs.style()) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Writing style", style = MaterialTheme.typography.titleMedium)
        Text("Local post-process only.", style = MaterialTheme.typography.bodySmall)
        styles.forEach { st ->
            val on = selected == st
            OpenButton(
                text = if (on) "✓ ${st.name}" else st.name,
                onClick = {
                    selected = st
                    prefs.styleName = st.name
                }
            )
        }
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
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tools & preferences",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsRow("Dictionary", "Custom words (local)", onDictionary)
        SettingsRow("Snippets", "Voice triggers → paste", onSnippets)
        SettingsRow("Style", "Casual · formal · excited", onStyle)
        SettingsRow("Appearance", "Light / dark · skin", onAppearance)
        SettingsRow("Bubble", "Size · opacity · shape", onBubble)
        SettingsRow("Cleanup", "Raw · light · normal · high", onCleanup)
        SettingsRow("Privacy", "STT disclaimer · retention", onPrivacy)
        SettingsRow("Sounds", "Haptics · cues", onSounds)
        SettingsRow("Home layout", "Show / hide cards", onHomeLayout)
        SettingsRow("Menu extras", "Optional items", onNavLayout)
        Text(
            "Local-first. No Open Flow account. System STT may leave device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        Text("Customize", style = MaterialTheme.typography.titleMedium)
        SettingsRow("Home layout", "Modules on Home", onHomeLayout)
        SettingsRow("Drawer extras", "What appears in the menu", onNavLayout)
    }
}

@Composable
private fun CleanupSettings(prefs: FlowPrefs) {
    var level by remember { mutableStateOf(prefs.cleanupLevel) }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cleanup", style = MaterialTheme.typography.titleMedium)
        Text(
            "How hard local polish runs before insert.",
            style = MaterialTheme.typography.bodySmall
        )
        listOf(
            "none" to "None — STT only",
            "light" to "Light — fillers",
            "medium" to "Medium — fillers · course-correct · punct",
            "high" to "High — medium + lists · stronger rules"
        ).forEach { (v, label) ->
            OpenButton(
                text = if (level == v) "✓ $label" else label,
                onClick = {
                    level = v
                    prefs.cleanupLevel = v
                }
            )
        }
    }
}

@Composable
private fun PrivacySettings(prefs: FlowPrefs) {
    var ret by remember { mutableStateOf(prefs.retentionPolicy) }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("History & privacy", style = MaterialTheme.typography.titleMedium)
        Text("Retention", style = MaterialTheme.typography.titleSmall)
        listOf(
            "keep" to "Keep forever",
            "wipe_24h" to "Wipe after 24h",
            "never_store" to "Never store history"
        ).forEach { (v, label) ->
            OpenButton(
                text = if (ret == v) "✓ $label" else label,
                onClick = {
                    ret = v
                    prefs.retentionPolicy = v
                }
            )
        }
        Text(
            "Export / share from History (on device). No cloud.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SoundsSettings(prefs: FlowPrefs) {
    var sounds by remember { mutableStateOf(prefs.bubbleSounds) }
    var haptics by remember { mutableStateOf(prefs.bubbleHaptics) }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sounds & haptics", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Start / stop sounds")
            OpenChip(label = if (sounds) "ON" else "OFF", isOn = sounds, onClick = {
                sounds = !sounds
                prefs.bubbleSounds = sounds
            })
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Haptics")
            OpenChip(label = if (haptics) "ON" else "OFF", isOn = haptics, onClick = {
                haptics = !haptics
                prefs.bubbleHaptics = haptics
            })
        }
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
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
        local.forEach { m ->
            val locked = m.id in lockVisible
            OpenCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        labels[m.id] ?: m.id,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OpenChip(
                            label = if (m.visible) "ON" else "OFF",
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
                        }) { Text("Up") }
                        TextButton(onClick = {
                            local = LayoutPrefs.move(local, m.id, 1)
                            onChange(local)
                        }) { Text("Down") }
                    }
                    if (locked) {
                        Text(
                            "Always available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    OpenCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Text("Visual skin", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpenButton(
                text = if (skin == VisualSkin.M3) "✓ M3 soft" else "M3 soft",
                onClick = { prefs.setVisualSkin(VisualSkin.M3) }
            )
            OpenButton(
                text = if (skin == VisualSkin.BRUTAL) "✓ Subtle brutal" else "Subtle brutal",
                onClick = { prefs.setVisualSkin(VisualSkin.BRUTAL) }
            )
        }
        Text("Light / dark", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (v, label) ->
                TextButton(onClick = { prefs.setDarkMode(v) }) {
                    Text(
                        label,
                        color = if (dark == v) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleSettings(prefs: FlowPrefs, onApplyBubble: () -> Unit) {
    var scale by remember { mutableFloatStateOf(prefs.bubbleScale) }
    var opacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var lang by remember { mutableStateOf(prefs.languageTag) }
    var mode by remember { mutableStateOf(prefs.bubbleMode) }
    var shape by remember { mutableStateOf(prefs.bubbleShape) }
    var showText by remember { mutableStateOf(prefs.bubbleShowText) }
    var snap by remember { mutableStateOf(prefs.bubbleEdgeSnap) }
    var haptics by remember { mutableStateOf(prefs.bubbleHaptics) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bubble", style = MaterialTheme.typography.titleMedium)
        Text(
            "Control chrome only by default — no speech caption.",
            style = MaterialTheme.typography.bodySmall
        )
        Text("Shape", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("circle" to "Circle", "pill" to "Pill", "square" to "Square", "dot" to "Dot")
                .forEach { (v, label) ->
                    TextButton(onClick = {
                        shape = v
                        prefs.bubbleShape = v
                        onApplyBubble()
                    }) {
                        Text(
                            label,
                            color = if (shape == v) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
        }
        Text("Idle size mode", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("full" to "Full", "compact" to "Compact", "dot" to "Dot").forEach { (v, label) ->
                TextButton(onClick = {
                    mode = v
                    prefs.bubbleMode = v
                    onApplyBubble()
                }) {
                    Text(
                        label,
                        color = if (mode == v) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text("Size ${(scale * 100).toInt()}%")
        Slider(
            value = scale,
            onValueChange = {
                scale = it
                prefs.bubbleScale = it
                onApplyBubble()
            },
            valueRange = 0.7f..1.15f
        )
        Text("Opacity ${(opacity * 100).toInt()}%")
        Slider(
            value = opacity,
            onValueChange = {
                opacity = it
                prefs.bubbleOpacity = it
                onApplyBubble()
            },
            valueRange = 0.2f..1f
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show speech on bubble")
                Text("Off = waveform / icon only", style = MaterialTheme.typography.bodySmall)
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
            Text("Edge snap")
            OpenChip(label = if (snap) "ON" else "OFF", isOn = snap, onClick = {
                snap = !snap
                prefs.bubbleEdgeSnap = snap
            })
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Haptics")
            OpenChip(label = if (haptics) "ON" else "OFF", isOn = haptics, onClick = {
                haptics = !haptics
                prefs.bubbleHaptics = haptics
            })
        }
        var pulse by remember { mutableStateOf(prefs.bubblePulse) }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Listen pulse")
            OpenChip(label = if (pulse) "ON" else "OFF", isOn = pulse, onClick = {
                pulse = !pulse
                prefs.bubblePulse = pulse
            })
        }
        OpenTextField(
            value = lang,
            onValueChange = {
                lang = it
                prefs.languageTag = it
            },
            label = "STT language tag e.g. en-US",
            supportingText = { Text("Offline pack may be required on device") }
        )
        OutlinedButton(
            onClick = {
                prefs.clearSnooze()
                onApplyBubble()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("End bubble snooze") }
    }
}
