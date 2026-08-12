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
import app.openflow.ui.theme.BrutalColors

/**
 * Modern brutal card — existing product skin (`VisualSkin.BRUTAL`).
 * Mockup truth: 3px border + hard offset shadow (no blur). Not soft M3.
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
    val borderWidth = 3.dp
    val borderColor = when {
        disabled -> BrutalColors.Charcoal.copy(alpha = 0.4f)
        selected -> BrutalColors.Ink
        else -> BrutalColors.Charcoal
    }
    val shadowColor = if (selected) BrutalColors.Ink else BrutalColors.Charcoal

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
            disabled -> BrutalColors.Stone.copy(alpha = 0.7f)
            else -> BrutalColors.Cream
        },
        contentColor = BrutalColors.OnCream,
        disabledContainerColor = BrutalColors.Stone.copy(alpha = 0.7f),
        disabledContentColor = BrutalColors.OnCream.copy(alpha = 0.38f)
    )

    val elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        disabledElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp
    )

    // Padding room for offset shadow; shadow block sits under face
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
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
