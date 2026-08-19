package app.openflow.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.openflow.data.DictationEntity
import app.openflow.data.ProcessStatus
import app.openflow.ui.HapticPick
import app.openflow.ui.LocalHapticTap
import app.openflow.ui.UiHapticMap
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.home.HistoryRowActions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DictationCard(
    d: DictationEntity,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSave: (oldText: String, newText: String) -> Unit,
    onUseRaw: (String) -> Unit,
) {
    var showRaw by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var draft by remember(d.id, d.text) { mutableStateOf(d.text) }
    val hasRaw = d.rawText.isNotBlank() && d.rawText != d.text
    val timeStr = remember(d.createdAtEpochMs) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(d.createdAtEpochMs))
    }
    val ctx = LocalContext.current
    val view = LocalView.current
    val hapticOn = HapticPick.constant(LocalHapticTap.current) != null

    fun copyText(value: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("dictation", value))
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }

    OpenCard {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            Text(
                "$timeStr · ${d.wordCount}w" +
                    if (ProcessStatus.isFailed(d.processStatus)) " · Fail" else "",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (ProcessStatus.isFailed(d.processStatus)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                softWrap = true
            )

            if (editing) {
                OpenTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = false,
                    minLines = 2,
                    showClearButton = false,
                    modifier = Modifier.testTag("history_edit")
                )
                OpenButton(
                    text = "Save",
                    onClick = {
                        val new = draft.trim()
                        if (new.isNotEmpty() && new != d.text) {
                            onSave(d.text, new)
                        }
                        editing = false
                    },
                    fill = false,
                    variant = ButtonVariant.Filled
                )
            } else {
                Text(
                    d.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = true,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            if (hapticOn) {
                                view.performHapticFeedback(
                                    UiHapticMap.constant(UiHapticMap.Event.COPY)
                                )
                            }
                            copyText(d.text)
                        }
                        .semantics { contentDescription = "Copy transcript" }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                CopyShareButtons(
                    onCopy = { copyText(d.text) },
                    onShare = onShare,
                )
                Box {
                    IconButton(
                        onClick = { menu = true },
                        modifier = Modifier
                            .size(Dimen.MIN_TOUCH)
                            .testTag("history_more")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        HistoryRowActions.more(hasRaw).forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    menu = false
                                    when (label) {
                                        "Edit" -> {
                                            draft = d.text
                                            editing = true
                                        }
                                        "Show raw" -> showRaw = !showRaw
                                        "Use raw" -> onUseRaw(d.rawText)
                                        "Delete" -> onDelete()
                                    }
                                },
                                modifier = if (label == "Use raw") Modifier.testTag("history_use_raw") else Modifier
                            )
                        }
                    }
                }
            }
            if (hasRaw && showRaw) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        d.rawText,
                        modifier = Modifier.padding(Dimen.GAP_SM),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                }
            }
        }
    }
}
