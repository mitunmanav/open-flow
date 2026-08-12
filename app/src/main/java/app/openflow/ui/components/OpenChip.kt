package app.openflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.theme.BrutalColors

/** Modern brutal chip (`VisualSkin.BRUTAL`): hard border, solid selected block. */
@Composable
fun OpenChip(
    label: String,
    isOn: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showCheckWhenOn: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOn) BrutalColors.Charcoal else BrutalColors.Cream,
        animationSpec = tween(100),
        label = "chip_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isOn) BrutalColors.OnCharcoal else BrutalColors.Charcoal,
        animationSpec = tween(100),
        label = "chip_text"
    )

    val borderColor = BrutalColors.Charcoal
    val borderWidth = 2.dp

    val stateLabel = if (isOn) "$label, selected" else label

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = Dimen.TOUCH_TARGET)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics {
                contentDescription = stateLabel
                role = Role.Checkbox
                selected = isOn
                if (!enabled) disabled()
            }
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        role = Role.Checkbox,
                        onClick = onClick
                    )
                } else Modifier
            ),
        color = bgColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimen.MIN_PADDING, vertical = Dimen.GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckWhenOn && isOn) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(Modifier.width(6.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isOn) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
