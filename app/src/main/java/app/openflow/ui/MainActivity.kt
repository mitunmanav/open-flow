package app.openflow.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.Modifier
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
import app.openflow.ui.theme.OpenFlowTheme
import kotlinx.coroutines.launch
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
            OpenFlowTheme {
                var tab by remember { mutableIntStateOf(0) }
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

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(tabTitle(tab)) })
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = { Icon(Icons.Default.Book, contentDescription = "Dictionary") },
                                label = { Text("Dictionary") }
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                icon = { Icon(Icons.Default.ShortText, contentDescription = "Snippets") },
                                label = { Text("Snippets") }
                            )
                            NavigationBarItem(
                                selected = tab == 3,
                                onClick = { tab = 3 },
                                icon = { Icon(Icons.Default.Style, contentDescription = "Style") },
                                label = { Text("Style") }
                            )
                            NavigationBarItem(
                                selected = tab == 4,
                                onClick = { tab = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { padding ->
                    when (tab) {
                        0 -> HomeTab(
                            modifier = Modifier.padding(padding),
                            app = app,
                            bubbleOn = bubbleOn,
                            micOn = micOn,
                            onEnableBubble = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) }
                        )
                        1 -> DictionaryTab(Modifier.padding(padding), app)
                        2 -> SnippetsTab(Modifier.padding(padding), app)
                        3 -> StyleTab(Modifier.padding(padding), app.prefs)
                        else -> SettingsTab(
                            Modifier.padding(padding),
                            app.prefs,
                            onApplyBubble = {
                                FlowAccessibilityService.instance?.applyPrefsVisual()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun tabTitle(tab: Int) = when (tab) {
        0 -> "Open Flow"
        1 -> "Dictionary"
        2 -> "Snippets"
        3 -> "Style"
        else -> "Settings"
    }
}

@Composable
private fun HomeTab(
    modifier: Modifier,
    app: OpenFlowApp,
    bubbleOn: Boolean,
    micOn: Boolean,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit
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
        modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Wispr-style bubble · fully local · no account",
            style = MaterialTheme.typography.titleMedium
        )
        Text(statsText, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(if (bubbleOn) "Bubble ON" else "Bubble OFF", bubbleOn)
            Chip(if (micOn) "Mic ON" else "Mic OFF", micOn)
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Setup", style = MaterialTheme.typography.titleSmall)
                Button(onClick = onEnableBubble, Modifier.fillMaxWidth()) {
                    Text(if (bubbleOn) "Accessibility settings" else "1. Enable Flow Bubble")
                }
                Button(onClick = onMic, Modifier.fillMaxWidth(), enabled = !micOn) {
                    Text(if (micOn) "Microphone granted" else "2. Grant microphone")
                }
                Text(
                    "3. Focus field → tap 🎙 (or hold to talk) → release/stop\n" +
                        "Drag bubble to bottom edge = snooze 10 min",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        OutlinedTextField(
            value = localNote,
            onValueChange = { localNote = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Test field") },
            minLines = 3
        )
        Text("History", style = MaterialTheme.typography.titleSmall)
        if (dictations.isEmpty()) {
            Text("No dictations yet. Use the bubble, then stop to save.")
        } else {
            dictations.take(30).forEach { d: DictationEntity ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${d.wordCount} words · ${d.languageTag}", style = MaterialTheme.typography.labelSmall)
                        Text(d.text.take(280), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            scope.launch { app.dictations.deleteDictation(d.id) }
                        }) { Text("Delete") }
                    }
                }
            }
        }
        Text(
            PrivacyDefaults().reportText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DictionaryTab(modifier: Modifier, app: OpenFlowApp) {
    val words by app.dictations.observeDictionary().collectAsState(initial = emptyList())
    var word by remember { mutableStateOf("") }
    var repl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Teach Open Flow your words (local)", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(word, { word = it }, Modifier.fillMaxWidth(), label = { Text("Word / phrase") })
        OutlinedTextField(repl, { repl = it }, Modifier.fillMaxWidth(), label = { Text("Replace with (optional)") })
        Button(
            onClick = {
                if (word.isNotBlank()) {
                    scope.launch {
                        app.dictations.addWord(word, repl.ifBlank { word })
                        word = ""
                        repl = ""
                    }
                }
            },
            Modifier.fillMaxWidth()
        ) { Text("Add") }
        words.forEach { w: DictionaryWordEntity ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${w.word} → ${w.replacement}")
                    OutlinedButton(onClick = {
                        scope.launch { app.dictations.deleteWord(w.id) }
                    }) { Text("Del") }
                }
            }
        }
    }
}

@Composable
private fun SnippetsTab(modifier: Modifier, app: OpenFlowApp) {
    val snippets by app.dictations.observeSnippets().collectAsState(initial = emptyList())
    var trigger by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Voice trigger → paste block (local)", style = MaterialTheme.typography.titleSmall)
        Text("Say the trigger alone as a full utterance after stop.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(trigger, { trigger = it }, Modifier.fillMaxWidth(), label = { Text("Trigger e.g. sig") })
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Body") }, minLines = 3)
        Button(
            onClick = {
                if (trigger.isNotBlank() && body.isNotBlank()) {
                    scope.launch {
                        app.dictations.addSnippet(trigger, body)
                        trigger = ""
                        body = ""
                    }
                }
            },
            Modifier.fillMaxWidth()
        ) { Text("Add snippet") }
        snippets.forEach { s: SnippetEntity ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun StyleTab(modifier: Modifier, prefs: FlowPrefs) {
    val styles = TextPostProcessor.Style.entries
    var selected by remember { mutableStateOf(prefs.style()) }
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Writing style (local post-process)", style = MaterialTheme.typography.titleSmall)
        styles.forEach { st ->
            val on = selected == st
            Button(
                onClick = {
                    selected = st
                    prefs.styleName = st.name
                },
                Modifier.fillMaxWidth()
            ) {
                Text(if (on) "✓ ${st.name}" else st.name)
            }
        }
    }
}

@Composable
private fun SettingsTab(modifier: Modifier, prefs: FlowPrefs, onApplyBubble: () -> Unit) {
    var scale by remember { mutableFloatStateOf(prefs.bubbleScale) }
    var opacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var lang by remember { mutableStateOf(prefs.languageTag) }
    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Flow Bubble (Wispr parity)", style = MaterialTheme.typography.titleSmall)
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
        OutlinedTextField(
            value = lang,
            onValueChange = {
                lang = it
                prefs.languageTag = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("STT language tag e.g. en-US, hi-IN") },
            supportingText = { Text("Default: ${Locale.getDefault().toLanguageTag()}") }
        )
        OutlinedButton(
            onClick = {
                prefs.clearSnooze()
                onApplyBubble()
            },
            Modifier.fillMaxWidth()
        ) { Text("End bubble snooze") }
        Text(
            "Local-first. Wispr needs cloud; we do not. MIT FOSS.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun Chip(label: String, ok: Boolean) {
    Surface(
        color = if (ok) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
