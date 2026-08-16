package app.openflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.a11y.OpenShapes
import app.openflow.ui.engine.EnginePreset

/**
 * Brutal read-only dropdown. One field → menu of [options].
 * Callers should omit dead stubs; [enabled] is a last-line guard only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenDropdown(
    label: String,
    selectedId: String,
    options: List<EnginePreset>,
    modifier: Modifier = Modifier,
    enabled: (String) -> Boolean = { true },
    groupOf: ((String) -> String?)? = null,
    testTag: String = "open_dropdown",
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label ?: selectedId

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().testTag(testTag)
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = OpenShapes.Field,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = scheme.outline,
                unfocusedBorderColor = scheme.outline,
                focusedContainerColor = scheme.surface,
                unfocusedContainerColor = scheme.surface,
                focusedLabelColor = scheme.onSurfaceVariant,
                unfocusedLabelColor = scheme.onSurfaceVariant,
                focusedTextColor = scheme.onSurface,
                unfocusedTextColor = scheme.onSurface,
                focusedTrailingIconColor = scheme.onSurface,
                unfocusedTrailingIconColor = scheme.onSurface,
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .testTag(testTag + "_field")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(testTag + "_menu")
        ) {
            var lastGroup: String? = null
            options.forEach { preset ->
                val group = groupOf?.invoke(preset.id)
                if (group != null && group != lastGroup) {
                    lastGroup = group
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurfaceVariant
                            )
                        },
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.testTag(
                            testTag + "_group_" + group.lowercase().replace(' ', '_')
                        )
                    )
                }
                val on = enabled(preset.id)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (on) preset.label else "${preset.label} (soon)",
                            fontWeight = if (preset.id == selectedId) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                            color = if (on) {
                                scheme.onSurface
                            } else {
                                scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    },
                    onClick = {
                        if (!on) return@DropdownMenuItem
                        onSelect(preset.id)
                        expanded = false
                    },
                    enabled = on,
                    modifier = Modifier.testTag(testTag + "_item_" + preset.id)
                )
            }
        }
    }
}
