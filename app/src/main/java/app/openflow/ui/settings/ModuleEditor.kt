package app.openflow.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.openflow.prefs.LayoutPrefs
import app.openflow.ui.HomeFeelCopy
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.home.ModuleEditorVisibility
import app.openflow.ui.theme.SecUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModuleEditor(
    title: String,
    subtitle: String,
    modules: List<LayoutPrefs.Module>,
    labels: Map<String, String>,
    lockVisible: Set<String> = emptySet(),
    defaultEncode: String? = null,
    onChange: (List<LayoutPrefs.Module>) -> Unit,
) {
    var local by remember(modules) { mutableStateOf(modules) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    SettingsPage(intro = subtitle) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SecUi.charcoal,
            softWrap = true,
        )
        Text(
            "Order = top to bottom on Home. Hide blocks you never use. Bottom tabs stay fixed.",
            style = MaterialTheme.typography.labelMedium,
            color = SecUi.muted,
            softWrap = true,
        )
        local.forEachIndexed { index, m ->
            val locked = m.id in lockVisible
            val what = HomeFeelCopy.moduleWhat(m.id)
            OpenCard(onClick = { focusedId = m.id }) {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${index + 1}. ${labels[m.id] ?: m.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SecUi.charcoal,
                            softWrap = true,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (m.visible) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (m.visible) SecUi.ink else SecUi.muted,
                        )
                    }
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    ) {
                        if (ModuleEditorVisibility.showHideChip(locked)) {
                            OpenChip(
                                label = if (m.visible) "Show" else "Hide",
                                isOn = m.visible,
                                modifier = Modifier.wrapContentHeight(),
                                onClick = {
                                    focusedId = m.id
                                    local = LayoutPrefs.toggleVisible(local, m.id)
                                    onChange(local)
                                },
                            )
                        }
                        OpenChip(
                            label = "↑ Up",
                            isOn = false,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                focusedId = m.id
                                local = LayoutPrefs.move(local, m.id, -1)
                                onChange(local)
                            },
                        )
                        OpenChip(
                            label = "↓ Down",
                            isOn = false,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                focusedId = m.id
                                local = LayoutPrefs.move(local, m.id, 1)
                                onChange(local)
                            },
                        )
                    }
                    if (focusedId == m.id && what.isNotEmpty()) {
                        Text(
                            what,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.charcoal,
                        )
                    }
                    if (locked) {
                        Text(
                            "Always available",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecUi.muted,
                        )
                    }
                }
            }
        }
        if (defaultEncode != null) {
            OpenButton(
                text = "Reset to default order",
                variant = ButtonVariant.Outlined,
                onClick = {
                    val catalog = if (defaultEncode == LayoutPrefs.DEFAULT_HOME) {
                        LayoutPrefs.HOME_MODULES
                    } else {
                        LayoutPrefs.DRAWER_EXTRAS
                    }
                    local = LayoutPrefs.parseModules(defaultEncode, catalog)
                    onChange(local)
                },
            )
        }
    }
}
