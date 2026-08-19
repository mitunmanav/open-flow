package app.openflow.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard

@Composable
fun LegalSectionCard(section: LegalCopy.Section, modifier: Modifier = Modifier) {
    OpenCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
        ) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                softWrap = true,
            )
            Text(
                section.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = true,
            )
        }
    }
}
