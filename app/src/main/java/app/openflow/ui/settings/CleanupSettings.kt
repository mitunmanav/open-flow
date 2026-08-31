package app.openflow.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard
import app.openflow.ui.theme.SecUi

@Composable
fun CleanupSettings(prefs: FlowPrefs) {
    var level by remember { mutableStateOf(prefs.cleanupLevel) }
    var spokenEmoji by remember { mutableStateOf(prefs.spokenEmoji) }
    SettingsPage(intro = "Real-time local text cleanup applied before inserting into fields.") {
        listOf(
            "none" to ("None" to "Exact speech — zero edits."),
            "light" to ("Light" to "Fillers + grammar: um/uh, repeats, spoken punct commands."),
            "medium" to ("Medium" to "Light + course-correct, false starts, lists, light clarity openers."),
            "high" to ("High" to "Medium + brevity hedges/wordiness (rules). Style still controls tone."),
        ).forEach { (v, pair) ->
            val (title, desc) = pair
            val on = level == v
            OpenCard(
                selected = on,
                onClick = {
                    level = v
                    prefs.cleanupLevel = v
                },
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(Dimen.MIN_PADDING),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SecUi.charcoal,
                            softWrap = true,
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true,
                        )
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = SecUi.charcoal,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        OpenCard(
            selected = spokenEmoji,
            onClick = {
                spokenEmoji = !spokenEmoji
                prefs.spokenEmoji = spokenEmoji
            },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Spoken emoji",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SecUi.charcoal,
                        softWrap = true,
                    )
                    Text(
                        "Opt-in: “smile emoji” → 😄 (local map, off by default).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted,
                        softWrap = true,
                    )
                }
                if (spokenEmoji) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = SecUi.charcoal,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
