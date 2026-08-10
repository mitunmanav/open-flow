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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.openflow.OpenFlowApp
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.data.SessionEntity
import app.openflow.privacy.PrivacyDefaults
import app.openflow.search.SearchHit
import app.openflow.search.TranscriptSearch
import app.openflow.ui.theme.OpenFlowTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* mic for STT when bubble listens */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OpenFlowApp
        setContent {
            OpenFlowTheme {
                OpenFlowHome(
                    sessionsFlow = app.sessions.observeSessions(),
                    bubbleRunning = FlowAccessibilityService.isRunning(),
                    onRequestMic = {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
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
    sessionsFlow: kotlinx.coroutines.flow.Flow<List<SessionEntity>>,
    bubbleRunning: Boolean,
    onRequestMic: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onSeedDemo: () -> Unit
) {
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    val privacy = remember { PrivacyDefaults() }
    val hits = remember(sessions, query) {
        TranscriptSearch.filter(
            sessions.map { SearchHit(it.id, it.title, it.transcript, it.createdAtEpochMs) },
            query
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Open Flow") })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Wispr-style dictation: floating bubble, not a keyboard. Local STT + private memory.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "How it works: keep your normal keyboard. Enable Flow Bubble. Tap a text field, tap the floating mic, speak — text inserts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (bubbleRunning) "Flow Bubble: ON" else "Flow Bubble: OFF (enable in Accessibility)",
                style = MaterialTheme.typography.labelLarge,
                color = if (bubbleRunning) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Text(
                privacy.reportText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
                Text("Enable Flow Bubble (Accessibility)")
            }
            Button(onClick = onRequestMic, modifier = Modifier.fillMaxWidth()) {
                Text("Grant microphone")
            }
            Button(onClick = onSeedDemo, modifier = Modifier.fillMaxWidth()) {
                Text("Add demo session (test storage)")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search transcripts") },
                singleLine = true
            )
            // Local test field: type here with bubble after a11y on
            var localNote by remember { mutableStateOf("") }
            OutlinedTextField(
                value = localNote,
                onValueChange = { localNote = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Test field — focus me, tap Flow bubble") },
                minLines = 2
            )
            if (hits.isEmpty()) {
                Text("No recordings yet. Tap demo or record later.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hits, key = { it.id }) { hit ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(hit.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hit.transcript.take(200),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
