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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import app.openflow.OpenFlowApp
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.data.SessionEntity
import app.openflow.privacy.PrivacyDefaults
import app.openflow.search.SearchHit
import app.openflow.search.TranscriptSearch
import app.openflow.ui.theme.OpenFlowTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OpenFlowApp
        setContent {
            OpenFlowTheme {
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
                        }
                    }
                    owner.lifecycle.addObserver(obs)
                    onDispose { owner.lifecycle.removeObserver(obs) }
                }

                OpenFlowHome(
                    sessionsFlow = app.sessions.observeSessions(),
                    bubbleOn = bubbleOn,
                    micOn = micOn,
                    onRequestMic = {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onOpenAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onSeedDemo = {
                        lifecycleScope.launch {
                            app.sessions.saveSession(
                                transcript = "Demo: open-flow local transcript for budget notes.",
                                audioPath = null,
                                durationMs = 5_000L,
                                languageTag = "en-US",
                                title = "Demo session"
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenFlowHome(
    sessionsFlow: Flow<List<SessionEntity>>,
    bubbleOn: Boolean,
    micOn: Boolean,
    onRequestMic: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onSeedDemo: () -> Unit
) {
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var localNote by remember { mutableStateOf("") }
    val privacy = remember { PrivacyDefaults() }
    val hits = remember(sessions, query) {
        TranscriptSearch.filter(
            sessions.map { SearchHit(it.id, it.title, it.transcript, it.createdAtEpochMs) },
            query
        )
    }
    val scroll = rememberScrollState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Open Flow") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Dictation anywhere — floating bubble, not a keyboard.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Speak as long as you want. Tap bubble to start, tap again to stop. OS pauses auto-restart.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(if (bubbleOn) "Bubble ON" else "Bubble OFF", bubbleOn)
                StatusChip(if (micOn) "Mic ON" else "Mic OFF", micOn)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Setup", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = onOpenAccessibility,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (bubbleOn) "Bubble settings" else "1. Enable Flow Bubble")
                    }
                    Button(
                        onClick = onRequestMic,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !micOn
                    ) {
                        Text(if (micOn) "Microphone granted" else "2. Grant microphone")
                    }
                    Text(
                        "3. Focus any text field → tap 🎙 bubble → talk → tap stop",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            OutlinedTextField(
                value = localNote,
                onValueChange = { localNote = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Test field — focus, then use bubble") },
                minLines = 3
            )
            Text(
                "Long dictation OK. Text lands in chunks as OS STT restarts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search memory") },
                singleLine = true
            )
            OutlinedButton(onClick = onSeedDemo, modifier = Modifier.fillMaxWidth()) {
                Text("Add demo session")
            }
            Text(
                privacy.reportText(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hits.isEmpty()) {
                Text("No sessions yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                hits.forEach { hit ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(hit.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                hit.transcript.take(240),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    Surface(
        color = if (ok) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
