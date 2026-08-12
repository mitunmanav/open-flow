package app.openflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OpenCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    disabled: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .then(
            if (contentDescription != null)
                Modifier.semantics { this.contentDescription = contentDescription }
            else Modifier
        )

    val colors = CardDefaults.cardColors(
        containerColor = when {
            disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        }
    )

    val elevation = CardDefaults.cardElevation(
        defaultElevation = if (selected) 2.dp else 1.dp
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = BorderStroke(1.dp, borderColor),
            elevation = elevation,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = BorderStroke(1.dp, borderColor),
            elevation = elevation,
            content = content
        )
    }
}
