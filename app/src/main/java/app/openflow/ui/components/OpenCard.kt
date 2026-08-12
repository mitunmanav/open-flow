package app.openflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen

/**
 * Minimal brutal card — cream face, hard 2dp border, 2dp offset block (no blur).
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
    val borderWidth = 2.dp
    val borderColor = when {
        disabled -> scheme.outline.copy(alpha = 0.4f)
        selected -> scheme.outline
        else -> scheme.outline
    }
    val shadowColor = scheme.outline

    val faceMod = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null && !disabled) Modifier.defaultMinSize(minHeight = Dimen.TOUCH_TARGET)
            else Modifier
        )
        .semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            if (disabled) this.disabled()
        }

    val colors = CardDefaults.cardColors(
        containerColor = when {
            disabled -> scheme.surfaceVariant.copy(alpha = 0.7f)
            selected -> scheme.surfaceVariant
            else -> scheme.surface
        },
        contentColor = scheme.onSurface,
        disabledContainerColor = scheme.surfaceVariant.copy(alpha = 0.7f),
        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f)
    )

    val elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        disabledElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp
    )

    // Minimal offset room (2dp) — not chunky 4dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 2.dp, bottom = 2.dp)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(2.dp, 2.dp)
                .background(shadowColor)
        )
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = faceMod,
                enabled = !disabled,
                shape = MaterialTheme.shapes.medium,
                colors = colors,
                border = BorderStroke(borderWidth, borderColor),
                elevation = elevation,
                content = content
            )
        } else {
            Card(
                modifier = faceMod,
                shape = MaterialTheme.shapes.medium,
                colors = colors,
                border = BorderStroke(borderWidth, borderColor),
                elevation = elevation,
                content = content
            )
        }
    }
}
