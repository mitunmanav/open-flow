package app.openflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.theme.SecUi

@Composable
fun SettingsHub(onOpen: (SettingsItem) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState())
            .testTag("settings_hub"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
    ) {
        Text(
            "Preferences & local configuration",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
        )
        SettingsCatalog.groups.forEach { group ->
            Spacer(Modifier.height(Dimen.GAP_SM))
            SettingsSectionTitle(group.title, group.id)
            group.items.forEach { item ->
                SettingsRow(
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = { onOpen(item) },
                    modifier = Modifier.testTag("settings_row_${item.name.lowercase()}"),
                )
            }
        }
        Text(
            "Open Flow is free and open source (MIT). No trackers. No analytics.",
            style = MaterialTheme.typography.labelSmall,
            color = SecUi.muted.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = Dimen.GAP_SM),
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}
