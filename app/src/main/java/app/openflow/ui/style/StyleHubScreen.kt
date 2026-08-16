package app.openflow.ui.style

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import app.openflow.prefs.FlowPrefs
import app.openflow.text.StyleCategory
import app.openflow.text.WritingStyle
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleHubScreen(prefs: FlowPrefs) {
    prefs.migrateLegacyAppContextIfNeeded()

    val scheme = MaterialTheme.colorScheme
    var category by remember { mutableStateOf(StyleCategory.PERSONAL) }
    var draftStyle by remember(category) {
        mutableStateOf(prefs.getHubStyle(category))
    }
    var savedStyle by remember(category) {
        mutableStateOf(prefs.getHubStyle(category))
    }
    var assignments by remember { mutableStateOf(prefs.getStyleAppAssignments()) }
    var showPicker by remember { mutableStateOf(false) }
    val dirty = draftStyle != savedStyle

    val allowed = WritingStyle.entries.filter { category.allows(it) && it != WritingStyle.CUSTOM }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            "Tone per app type. Local rules only — no AI rewrite on Style.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            softWrap = true
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            StyleCategory.entries.forEach { cat ->
                OpenChip(
                    label = cat.label,
                    isOn = category == cat,
                    onClick = {
                        category = cat
                        val s = prefs.getHubStyle(cat)
                        draftStyle = s
                        savedStyle = s
                    }
                )
            }
        }

        Text(
            when (category) {
                StyleCategory.PERSONAL -> "Messages, WhatsApp, Telegram, Signal, Discord…"
                StyleCategory.WORK -> "Slack, Teams, LinkedIn…"
                StyleCategory.EMAIL -> "Gmail, Outlook, Proton Mail…"
                StyleCategory.OTHER -> "Everything else"
            },
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )

        allowed.forEach { st ->
            val on = draftStyle == st
            OpenCard(
                selected = on,
                onClick = { draftStyle = st }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(Dimen.MIN_PADDING),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            styleLabel(st),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground
                        )
                        Text(
                            styleDesc(st),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = scheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            OpenButton(
                text = "Save",
                onClick = {
                    prefs.setHubStyle(category, draftStyle)
                    savedStyle = draftStyle
                },
                enabled = dirty,
                modifier = Modifier.weight(1f)
            )
            if (dirty) {
                OpenButton(
                    text = "Discard",
                    onClick = { draftStyle = savedStyle },
                    variant = ButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            "Your apps in ${category.label}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Text(
            "Add apps we don’t auto-detect, or move one into this category.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        val inCat = assignments.filter { it.value == category }.keys.sorted()
        if (inCat.isEmpty()) {
            Text(
                "No custom apps yet.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        } else {
            val pm = LocalContext.current.packageManager
            inCat.forEach { pkg ->
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                OpenCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(Dimen.MIN_PADDING),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onBackground
                            )
                            Text(
                                pkg,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                prefs.removeStyleAppAssignment(pkg)
                                assignments = prefs.getStyleAppAssignments()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = scheme.error
                            )
                        }
                    }
                }
            }
        }

        OpenButton(
            text = "+ Add your app",
            onClick = { showPicker = true },
            variant = ButtonVariant.Outlined
        )

        Text(
            "Styles work best for English dictation.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }

    if (showPicker) {
        AddAppDialog(
            already = assignments.keys,
            onDismiss = { showPicker = false },
            onPick = { pkg ->
                prefs.setStyleAppAssignment(pkg, category)
                assignments = prefs.getStyleAppAssignments()
                showPicker = false
            }
        )
    }
}

@Composable
private fun AddAppDialog(
    already: Set<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val alreadyLower = remember(already) { already.map { it.lowercase() }.toSet() }
    val apps = remember {
        LauncherAppQuery.list(context.packageManager)
            .filterNot { it.packageName.lowercase() in alreadyLower }
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) { LauncherAppQuery.filter(apps, query) }

    Dialog(onDismissRequest = onDismiss) {
        OpenCard {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING)
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Pick an app",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                OpenTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search name or package",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app.packageName) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                        ) {
                            if (app.icon != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            setImageDrawable(app.icon)
                                            scaleType = ImageView.ScaleType.FIT_CENTER
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scheme.onBackground
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                OpenButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = ButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun styleLabel(st: WritingStyle): String = when (st) {
    WritingStyle.FORMAL -> "Formal"
    WritingStyle.CASUAL -> "Casual"
    WritingStyle.VERY_CASUAL -> "Very casual"
    WritingStyle.EXCITED -> "Excited"
    WritingStyle.CUSTOM -> "Custom"
}

private fun styleDesc(st: WritingStyle): String = when (st) {
    WritingStyle.FORMAL -> "Sentence case, ends with ., expands informal."
    WritingStyle.CASUAL -> "Everyday tone, sentence case."
    WritingStyle.VERY_CASUAL -> "Chat-like: soft caps, no forced period."
    WritingStyle.EXCITED -> "High energy, prefers !"
    WritingStyle.CUSTOM -> "Your custom rules."
}
