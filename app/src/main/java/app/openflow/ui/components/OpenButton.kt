package app.openflow.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.theme.BrutalColors

enum class ButtonVariant { Filled, Outlined, Text }

/** Modern brutal CTA (`VisualSkin.BRUTAL`): solid charcoal, hard edge. */
@Composable
fun OpenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val view = LocalView.current
    val shape = MaterialTheme.shapes.small
    val semanticsMod = Modifier.semantics {
        this.contentDescription = contentDescription ?: text
    }
    val minTouch = Modifier.defaultMinSize(
        minWidth = Dimen.TOUCH_TARGET,
        minHeight = Dimen.TOUCH_TARGET
    )
    val contentPad = PaddingValues(horizontal = Dimen.MIN_PADDING, vertical = 0.dp)
    val disabledContainer = BrutalColors.Charcoal.copy(alpha = 0.12f)
    val disabledContent = BrutalColors.Charcoal.copy(alpha = 0.38f)

    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = {
                performClickHaptic(view)
                onClick()
            },
            modifier = modifier.fillMaxWidth().then(minTouch).then(semanticsMod),
            enabled = enabled,
            shape = shape,
            contentPadding = contentPad,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrutalColors.Charcoal,
                contentColor = BrutalColors.OnCharcoal,
                disabledContainerColor = disabledContainer,
                disabledContentColor = disabledContent
            )
        ) { Text(text) }

        ButtonVariant.Outlined -> OutlinedButton(
            onClick = {
                performClickHaptic(view)
                onClick()
            },
            modifier = modifier.fillMaxWidth().then(minTouch).then(semanticsMod),
            enabled = enabled,
            shape = shape,
            contentPadding = contentPad,
            border = BorderStroke(
                width = 2.dp,
                color = if (enabled) BrutalColors.Charcoal else BrutalColors.Charcoal.copy(alpha = 0.35f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = BrutalColors.Cream,
                contentColor = BrutalColors.Charcoal,
                disabledContentColor = disabledContent
            )
        ) { Text(text) }

        ButtonVariant.Text -> TextButton(
            onClick = {
                performClickHaptic(view)
                onClick()
            },
            modifier = modifier.then(minTouch).then(semanticsMod),
            enabled = enabled,
            shape = shape,
            contentPadding = contentPad,
            colors = ButtonDefaults.textButtonColors(
                contentColor = BrutalColors.Charcoal,
                disabledContentColor = disabledContent
            )
        ) { Text(text) }
    }
}

private fun performClickHaptic(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
