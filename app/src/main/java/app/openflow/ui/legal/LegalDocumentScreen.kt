package app.openflow.ui.legal

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.openflow.ui.settings.SettingsPage
import app.openflow.ui.theme.SecUi

@Composable
fun LegalDocumentScreen(
    title: String,
    intro: String,
    sections: List<LegalCopy.Section>,
    tag: String,
) {
    SettingsPage(tag = tag) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = SecUi.charcoal,
            modifier = Modifier.testTag(tag),
            softWrap = true,
        )
        Text(
            intro,
            style = MaterialTheme.typography.bodyMedium,
            color = SecUi.charcoal,
            softWrap = true,
        )
        sections.forEach { LegalSectionCard(it) }
    }
}
