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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.a11y.OpenShapes

enum class ButtonVariant { Filled, Outlined, Text }

/** Modern brutal CTA (`VisualSkin.BRUTAL`): solid primary, hard edge. Theme-aware. */
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
    val scheme = MaterialTheme.colorScheme
    val shape = OpenShapes.Button
    val semanticsMod = Modifier.semantics {
        this.contentDescription = contentDescription ?: text
    }
    val minTouch = Modifier.defaultMinSize(
        minWidth = Dimen.TOUCH_TARGET,
        minHeight = Dimen.TOUCH_TARGET
    )
    val contentPad = PaddingValues(horizontal = Dimen.MIN_PADDING, vertical = 0.dp)
    val disabledContainer = scheme.onSurface.copy(alpha = 0.12f)
    val disabledContent = scheme.onSurface.copy(alpha = 0.38f)

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
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = disabledContainer,
                disabledContentColor = disabledContent
            )
        ) { Text(text, overflow = TextOverflow.Visible, softWrap = true) }

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
                color = if (enabled) scheme.outline else scheme.outline.copy(alpha = 0.35f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = scheme.surface,
                contentColor = scheme.onSurface,
                disabledContentColor = disabledContent
            )
        ) { Text(text, overflow = TextOverflow.Visible, softWrap = true) }

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
                contentColor = scheme.onSurface,
                disabledContentColor = disabledContent
            )
        ) { Text(text, overflow = TextOverflow.Visible, softWrap = true) }
    }
}

private fun performClickHaptic(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
