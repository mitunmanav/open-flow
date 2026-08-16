package app.openflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen

@Composable
fun OpenListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    val a11y = contentDescription
        ?: if (subtitle.isNullOrBlank()) title else "$title. $subtitle"

    val clickMod = if (onClick != null) {
        Modifier
            .defaultMinSize(minHeight = Dimen.TOUCH_TARGET)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickMod)
            .semantics {
                this.contentDescription = a11y
                if (onClick != null) role = Role.Button
            }
            .padding(horizontal = Dimen.MIN_PADDING, vertical = Dimen.GAP),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Visible,
                softWrap = true,
                color = MaterialTheme.colorScheme.onSurface
            )
            actions?.invoke()
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Visible,
                softWrap = true,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
