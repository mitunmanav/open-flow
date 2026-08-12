package app.openflow.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.openflow.ui.a11y.Dimen

/** Theme-aware brutal field: surface fill, outline border. */
@Composable
fun OpenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    showClearButton: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    contentDescription: String? = null,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    supportingText: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val effectiveSingleLine = singleLine && minLines <= 1
    val effectiveMaxLines = when {
        effectiveSingleLine -> 1
        else -> maxLines.coerceAtLeast(minLines)
    }
    val a11yLabel = contentDescription ?: label ?: placeholder
    val resolvedKeyboard = keyboardOptions ?: KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        keyboardType = KeyboardType.Text,
        imeAction = if (effectiveSingleLine) ImeAction.Done else ImeAction.Default
    )
    val muted = scheme.onSurfaceVariant.copy(alpha = 0.75f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimen.TOUCH_TARGET)
            .then(
                if (a11yLabel != null)
                    Modifier.semantics { this.contentDescription = a11yLabel }
                else Modifier
            ),
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let {
            {
                Text(it, color = muted)
            }
        },
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(error, color = scheme.error) }
            }
            supportingText != null -> supportingText
            else -> null
        },
        singleLine = effectiveSingleLine,
        minLines = if (effectiveSingleLine) 1 else minLines,
        maxLines = effectiveMaxLines,
        shape = MaterialTheme.shapes.small,
        leadingIcon = leadingIcon,
        trailingIcon = if (showClearButton && value.isNotEmpty() && enabled) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.defaultMinSize(
                        minWidth = Dimen.TOUCH_TARGET,
                        minHeight = Dimen.TOUCH_TARGET
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear text",
                        tint = muted
                    )
                }
            }
        } else null,
        keyboardOptions = resolvedKeyboard,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = scheme.outline,
            unfocusedBorderColor = scheme.outline,
            disabledBorderColor = scheme.outline.copy(alpha = 0.35f),
            errorBorderColor = scheme.error,
            focusedContainerColor = scheme.surface,
            unfocusedContainerColor = scheme.surface,
            disabledContainerColor = scheme.surfaceVariant.copy(alpha = 0.45f),
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            disabledTextColor = scheme.onSurface.copy(alpha = 0.38f),
            focusedLabelColor = scheme.onSurface,
            unfocusedLabelColor = muted,
            disabledLabelColor = scheme.onSurface.copy(alpha = 0.38f),
            disabledPlaceholderColor = scheme.onSurface.copy(alpha = 0.38f),
            cursorColor = scheme.secondary,
            errorCursorColor = scheme.error,
            focusedTrailingIconColor = muted,
            unfocusedTrailingIconColor = muted
        )
    )
}
