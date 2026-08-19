package app.openflow.ui.haptics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.openflow.ui.HapticFeel
import app.openflow.ui.HapticPick
import app.openflow.ui.HapticUiCopy
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HapticCustomPanel(
    picks: Map<HapticFeel.Event, String>,
    onPick: (HapticFeel.Event, String) -> Unit,
    onTest: (HapticFeel.Event) -> Unit,
) {
    val chips = listOf(
        HapticPick.OFF,
        HapticPick.TICK,
        HapticPick.CLICK,
        HapticPick.CONFIRM,
        HapticPick.REJECT,
    )
    Column(
        modifier = Modifier.testTag("haptics_custom_panel"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
    ) {
        HapticFeel.Event.entries.forEach { event ->
            val copy = HapticUiCopy.event(event)
            OpenCard {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    Text(copy.first, style = MaterialTheme.typography.titleSmall, softWrap = true)
                    Text(
                        copy.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chips.forEach { id ->
                            OpenChip(
                                label = HapticUiCopy.pick(id),
                                isOn = picks[event] == id,
                                onClick = { onPick(event, id) },
                            )
                        }
                    }
                    OpenButton(
                        text = "Test",
                        onClick = { onTest(event) },
                        variant = ButtonVariant.Outlined,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("haptics_test_${event.name.lowercase()}"),
                    )
                }
            }
        }
    }
}
