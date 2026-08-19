package app.openflow.ui.haptics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.HapticFeel
import app.openflow.ui.HapticPick
import app.openflow.ui.HapticUiCopy
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.settings.SettingsPage
import app.openflow.ui.theme.SecUi

@Composable
fun HapticsSettings(prefs: FlowPrefs, onTapPick: (String) -> Unit) {
    var feel by remember { mutableStateOf(prefs.hapticFeel) }
    var picks by remember {
        mutableStateOf(HapticFeel.Event.entries.associateWith { prefs.hapticPick(it) })
    }
    val view = LocalView.current

    fun play(event: HapticFeel.Event) {
        val c = HapticPick.constant(prefs.hapticPick(event)) ?: return
        view.performHapticFeedback(c)
    }

    SettingsPage(intro = HapticUiCopy.PAGE) {
        HapticPresetSelector(
            feel = feel,
            onPick = { id ->
                feel = id
                prefs.hapticFeel = id
                picks = HapticFeel.Event.entries.associateWith { prefs.hapticPick(it) }
                onTapPick(prefs.hapticPick(HapticFeel.Event.TAP))
            },
        )
        Text(
            HapticUiCopy.preset(feel),
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
        )
        OpenButton(
            text = "Test",
            onClick = { play(HapticFeel.Event.TAP) },
            variant = ButtonVariant.Outlined,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("haptics_test"),
        )
        OpenButton(
            text = "Reset",
            onClick = {
                prefs.resetHaptics()
                feel = prefs.hapticFeel
                picks = HapticFeel.Event.entries.associateWith { prefs.hapticPick(it) }
                onTapPick(prefs.hapticPick(HapticFeel.Event.TAP))
            },
            variant = ButtonVariant.Outlined,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("haptics_reset"),
        )
        if (feel == HapticFeel.CUSTOM) {
            HapticCustomPanel(
                picks = picks,
                onPick = { event, id ->
                    prefs.setHapticPick(event, id)
                    picks = picks + (event to id)
                    if (event == HapticFeel.Event.TAP) onTapPick(id)
                },
                onTest = { play(it) },
            )
        }
    }
}
