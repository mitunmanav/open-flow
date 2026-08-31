package app.openflow.ui.snippets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import app.openflow.OpenFlowApp
import app.openflow.data.SnippetEntity
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.dictionary.PairImportBlock
import app.openflow.ui.home.HubListPolicy
import app.openflow.ui.home.UiScrollPolicy
import app.openflow.ui.theme.SecUi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun SnippetsTab(app: OpenFlowApp) {
    val snippets by app.dictations.observeSnippets().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var trigger by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val shown = remember(snippets, query) {
        snippets.filter { HubListPolicy.matches(query, it.trigger, it.body) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item(key = "snip-search") {
                OpenTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search snippets…",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SecUi.muted
                        )
                    },
                    modifier = Modifier.testTag("snippet_search")
                )
            }

            if (shown.isEmpty()) {
                item(key = "snip-empty") {
                    EmptyState(
                        icon = Icons.Default.Tune,
                        title = if (query.isBlank()) "No voice snippets" else "No matching results",
                        subtitle = if (query.isBlank()) {
                            "Tap + to add a trigger and paste block."
                        } else {
                            "Try a different search keyword."
                        }
                    )
                }
            } else {
                items(
                    items = shown,
                    key = { UiScrollPolicy.snippetRowKey(it.id) },
                    contentType = { "snip" },
                ) { s: SnippetEntity ->
                    OpenCard {
                        Column(
                            Modifier.padding(Dimen.MIN_PADDING),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = Dimen.MIN_TOUCH),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Trigger: \"${s.trigger}\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecUi.charcoal,
                                    softWrap = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { scope.launch { app.dictations.deleteSnippet(s.id) } },
                                    modifier = Modifier.size(Dimen.MIN_TOUCH)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SecUi.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                if (s.body.length > 200) "${s.body.take(200)}…" else s.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecUi.muted,
                                softWrap = true
                            )
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
                .testTag("snippet_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add snippet")
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
                            "New snippet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        OpenTextField(
                            value = trigger,
                            onValueChange = { trigger = it },
                            label = "Trigger",
                            placeholder = "Trigger (e.g. my address, email sig)"
                        )
                        OpenTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = "Paste block",
                            placeholder = "Expansion text…",
                            singleLine = false,
                            minLines = 3
                        )
                        OpenButton(
                            text = "Add snippet",
                            modifier = Modifier.testTag("snippet_add"),
                            enabled = trigger.isNotBlank() && body.isNotBlank(),
                            onClick = {
                                if (trigger.isNotBlank() && body.isNotBlank()) {
                                    scope.launch {
                                        val ok = app.dictations.addSnippet(trigger.trim(), body.trim())
                                        if (!ok) {
                                            Toast.makeText(
                                                ctx,
                                                "That trigger is a dictionary word",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            trigger = ""
                                            body = ""
                                            showAdd = false
                                        }
                                    }
                                }
                            }
                        )
                        PairImportBlock(
                            testPrefix = "snippet",
                            onImport = { app.dictations.importSnippets(it) }
                        )
                        OpenButton(
                            text = "Close",
                            variant = ButtonVariant.Outlined,
                            onClick = { showAdd = false },
                            modifier = Modifier.testTag("snippet_add_close")
                        )
                    }
                }
            }
        }
    }
}
