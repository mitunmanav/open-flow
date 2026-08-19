package app.openflow.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.theme.SecUi

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flat row — no offset-shadow OpenCard (many shadows = Settings jank).
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Dimen.MIN_TOUCH)
            .border(SecUi.hardBorder)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimen.MIN_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SecUi.charcoal,
                softWrap = true,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
                maxLines = 2,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open $title",
            tint = SecUi.muted,
        )
    }
}
