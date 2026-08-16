package app.openflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.a11y.OpenShapes

/**
 * Minimal brutal card — cream face, hard 2dp border, 2dp offset block (no blur).
 * No Material Card/Surface: those clip children to [shape] and cut chips/text.
 * Colors follow [MaterialTheme.colorScheme] so light/dark work.
 */
@Composable
fun OpenCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    disabled: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val borderColor = when {
        disabled -> scheme.outline.copy(alpha = 0.3f)
        selected -> scheme.primary
        else -> scheme.outline.copy(alpha = 0.5f)
    }
    val faceColor = when {
        disabled -> scheme.surfaceVariant.copy(alpha = 0.6f)
        selected -> scheme.surfaceVariant
        else -> scheme.surface
    }

    val faceMod = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null && !disabled) Modifier.defaultMinSize(minHeight = Dimen.MIN_TOUCH)
            else Modifier
        )
        .background(color = faceColor, shape = shape)
        .border(BorderStroke(1.dp, borderColor), shape)
        .semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            if (disabled) this.disabled()
            if (onClick != null) role = Role.Button
        }
        .then(
            if (onClick != null && !disabled) {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            } else Modifier
        )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = faceMod,
            content = content
        )
    }
}
