package app.openflow.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.openflow.ui.theme.SecUi

@Composable
fun SettingsSectionTitle(title: String, groupId: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = SecUi.charcoal,
        modifier = Modifier.testTag("settings_group_$groupId"),
        softWrap = true,
    )
}
