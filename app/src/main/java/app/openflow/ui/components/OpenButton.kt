package app.openflow.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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

enum class ButtonVariant { Filled, Outlined, Text }

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
    val semanticsMod = if (contentDescription != null)
        Modifier.semantics { this.contentDescription = contentDescription }
    else Modifier

    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = {
                performClickHaptic(view)
                onClick()
            },
            modifier = modifier.fillMaxWidth().height(Dimen.TOUCH_TARGET).then(semanticsMod),
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { Text(text) }

        ButtonVariant.Outlined -> OutlinedButton(
            onClick = {
                performClickHaptic(view)
                onClick()
            },
            modifier = modifier.fillMaxWidth().height(Dimen.TOUCH_TARGET).then(semanticsMod),
            enabled = enabled
        ) { Text(text) }

        ButtonVariant.Text -> TextButton(
            onClick = {
                performClickHaptic(view)
                onClick()
            },
            modifier = modifier.height(Dimen.TOUCH_TARGET).then(semanticsMod),
            enabled = enabled
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
