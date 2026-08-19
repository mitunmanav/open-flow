package app.openflow.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.theme.SecUi

@Composable
fun SoundsSettings(prefs: FlowPrefs) {
    var sounds by remember { mutableStateOf(prefs.bubbleSounds) }
    SettingsPage(intro = "Audio cues when dictation starts or stops.") {
        OpenCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING)
                    .heightIn(min = Dimen.MIN_TOUCH),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Start / Stop Audio Cue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.charcoal,
                        softWrap = true,
                    )
                    Text(
                        "Play a short tone when dictation starts or stops",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted,
                        softWrap = true,
                    )
                }
                OpenChip(
                    label = if (sounds) "ON" else "OFF",
                    isOn = sounds,
                    onClick = {
                        sounds = !sounds
                        prefs.bubbleSounds = sounds
                    },
                )
            }
        }
    }
}
