package app.openflow.ui.haptics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.openflow.ui.HapticFeel
import app.openflow.ui.components.OpenChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HapticPresetSelector(
    feel: String,
    onPick: (String) -> Unit,
) {
    val options = listOf(
        HapticFeel.OFF to "Off",
        HapticFeel.LIGHT to "Light",
        HapticFeel.FULL to "Full",
        HapticFeel.CUSTOM to "Custom",
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            OpenChip(
                label = label,
                isOn = feel == id,
                onClick = { onPick(id) },
                modifier = Modifier.testTag("haptics_preset_$id"),
            )
        }
    }
}
