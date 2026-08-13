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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    val borderColor = when {
        disabled -> scheme.outline.copy(alpha = 0.4f)
        else -> scheme.outline
    }
    val shadowColor = scheme.outline
    val faceColor = when {
        disabled -> scheme.surfaceVariant.copy(alpha = 0.7f)
        selected -> scheme.surfaceVariant
        else -> scheme.surface
    }

    val faceMod = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null && !disabled) Modifier.defaultMinSize(minHeight = Dimen.TOUCH_TARGET)
            else Modifier
        )
        .graphicsLayer { clip = false }
        .background(color = faceColor, shape = OpenShapes.Card)
        .border(BorderStroke(Dimen.BORDER, borderColor), OpenShapes.Card)
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

    // Minimal offset room (2dp) — not chunky 4dp. clip off so chips/text not cut.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { clip = false }
            .padding(end = 2.dp, bottom = 2.dp)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(2.dp, 2.dp)
                .background(shadowColor)
        )
        Column(
            modifier = faceMod,
            content = content
        )
    }
}
