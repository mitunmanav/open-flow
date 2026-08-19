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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.R
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard
import app.openflow.ui.privacy.PrivacyHonesty
import app.openflow.ui.theme.SecUi

@Composable
fun PrivacySettings(prefs: FlowPrefs) {
    var ret by remember { mutableStateOf(prefs.retentionPolicy) }
    var autoLearn by remember { mutableStateOf(prefs.autoLearn) }
    SettingsPage {
        Text(
            stringResource(R.string.privacy_no_internet),
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
            modifier = Modifier.testTag("privacy_internet_honesty"),
        )
        Text(
            PrivacyHonesty.SETTINGS_BODY,
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
        )
        OpenCard(
            selected = autoLearn,
            onClick = {
                autoLearn = !autoLearn
                prefs.autoLearn = autoLearn
            },
            modifier = Modifier.testTag("privacy_auto_learn"),
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
                        "Auto-learn from fixes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.charcoal,
                        softWrap = true,
                    )
                    Text(
                        "When you correct a word after dictation, remember it. Off = no new pairs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted,
                        softWrap = true,
                    )
                }
                if (autoLearn) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "On",
                        tint = SecUi.charcoal,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        listOf(
            "keep" to ("Keep forever" to PrivacyHonesty.KEEP_FOREVER),
            "wipe_24h" to ("Wipe after 24h" to "Delete dictations older than 24 hours on each new save."),
            "never_store" to ("Never store" to "Do not write history. Last-session copy still available until you clear it."),
        ).forEach { (v, pair) ->
            val (title, desc) = pair
            val on = ret == v
            OpenCard(
                selected = on,
                onClick = {
                    ret = v
                    prefs.retentionPolicy = v
                },
                modifier = Modifier.testTag("privacy_" + v),
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
                            fontWeight = FontWeight.Bold,
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
    }
}
