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
import app.openflow.privacy.PrivacyDefaults
import app.openflow.text.TextPostProcessor
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.theme.OpenFlowTheme
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
            OpenFlowTheme(darkMode = darkMode) {
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

                AppShell(
                    route = route,
                    onNavigate = { route = it }
                ) { padding ->
                    AnimatedContent(
                        targetState = route,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        },
                        label = "route_content",
                        modifier = Modifier.padding(padding)
                    ) { r ->
                        when (r) {
                            AppRoute.Home -> HomeHub(
                                app = app,
                                bubbleOn = bubbleOn,
                                micOn = micOn,
                                onEnableBubble = {
                                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                                onOpenHistory = { route = AppRoute.History }
                            )
                            AppRoute.History -> HistoryScreen(app)
                            AppRoute.Dictionary -> DictionaryTab(app)
                            AppRoute.Snippets -> SnippetsTab(app)
                            AppRoute.Style -> StyleTab(app.prefs)
                            AppRoute.Settings -> SettingsHub(
                                onAppearance = { route = AppRoute.Appearance },
                                onBubble = { route = AppRoute.BubbleSettings }
                            )
                            AppRoute.Appearance -> AppearanceSettings(app.prefs)
                            AppRoute.BubbleSettings -> BubbleSettings(
                                prefs = app.prefs,
                                onApplyBubble = {
                                    FlowAccessibilityService.instance?.applyPrefsVisual()
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
    onOpenHistory: () -> Unit
) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var statsText by remember { mutableStateOf("…") }
    var localNote by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        scope.launch {
            val s = app.dictations.stats()
            statsText = "Words ${s.totalWords} · Sessions ${s.totalSessions} · Streak ${s.streakDays}d"
        }
        onDispose { }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Speak anywhere. Stay private.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Floating bubble · on-device STT · no account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Setup", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpenChip(label = if (bubbleOn) "Bubble ON" else "Bubble OFF", isOn = bubbleOn)
                    OpenChip(label = if (micOn) "Mic ON" else "Mic OFF", isOn = micOn)
                }
                OpenButton(
                    text = if (bubbleOn) "Accessibility settings" else "1. Enable Flow Bubble",
                    onClick = onEnableBubble
                )
                OpenButton(
                    text = if (micOn) "Microphone granted" else "2. Grant microphone",
                    onClick = onMic,
                    enabled = !micOn
                )
                Text(
                    "Focus a field → tap 🎙 bubble (hold to talk). Spoken words show on the bubble.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your pace", style = MaterialTheme.typography.titleSmall)
                Text(statsText, style = MaterialTheme.typography.bodyLarge)
            }
        }

        OpenTextField(
            value = localNote,
            onValueChange = { localNote = it },
            label = "Test field — try the bubble here",
            minLines = 3
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenHistory) { Text("See all") }
        }
        if (dictations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = "No dictations yet",
                subtitle = "Use the bubble, then stop to save."
            )
        } else {
            dictations.take(8).forEach { d: DictationEntity ->
                DictationCard(d, onDelete = {
                    scope.launch { app.dictations.deleteDictation(d.id) }
                })
            }
        }

        Text(
            PrivacyDefaults.reportText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
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
    OpenCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${d.wordCount} words · ${d.languageTag}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(d.text.take(400), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyButton(text = d.text)
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CopyButton(text: String) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("dictation", text))
        }
    ) { Text("Copy") }
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
    onAppearance: () -> Unit,
    onBubble: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleMedium)
        SettingsRow("Appearance", "Theme light / dark / system", onAppearance)
        SettingsRow("Bubble", "Size, opacity, shape, language", onBubble)
        OpenCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coming in Drop 2", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Home modules · drawer item visibility · more bubble knobs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "Local-first. MIT FOSS. No account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Text("Calm pro · system respects device theme", style = MaterialTheme.typography.bodySmall)
        Text("Theme", style = MaterialTheme.typography.titleSmall)
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bubble", style = MaterialTheme.typography.titleMedium)
        Text("Live transcript shows on the bubble while you speak.", style = MaterialTheme.typography.bodySmall)
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
        Text("Shape", style = MaterialTheme.typography.titleSmall)
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
        OpenTextField(
            value = lang,
            onValueChange = {
                lang = it
                prefs.languageTag = it
            },
            label = "STT language tag e.g. en-US, hi-IN",
            supportingText = { Text("Default: ${java.util.Locale.getDefault().toLanguageTag()}") }
        )
        OutlinedButton(
            onClick = {
                prefs.clearSnooze()
                onApplyBubble()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("End bubble snooze") }
        Text(
            "Hides on bank/auth apps. Shake to unsnooze.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
