package app.openflow.ui.settings

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.openflow.display.DisplayRefreshController
import app.openflow.display.DisplayRefreshPolicy
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.theme.SecUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettings(prefs: FlowPrefs) {
    val dark by prefs.darkMode.collectAsState()
    val context = LocalContext.current
    var refreshHz by remember { mutableIntStateOf(prefs.refreshHz) }
    val deviceModes = remember(context) {
        try {
            val d = if (Build.VERSION.SDK_INT >= 30) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay
            }
            d?.supportedModes?.map {
                DisplayRefreshPolicy.ModeInfo(it.modeId, it.refreshRate)
            }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }
    val hzChoices = remember(deviceModes) {
        DisplayRefreshPolicy.availableTargets(deviceModes).ifEmpty {
            DisplayRefreshPolicy.TARGETS_HZ
        }
    }
    SettingsPage(intro = "Theme and display refresh. Changes apply now.") {
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Color theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecUi.charcoal,
                    softWrap = true,
                )
                Text(
                    "Light / Dark / System — all screens + bubble chrome follow this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted,
                    softWrap = true,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .testTag("appearance_theme"),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (v, label) ->
                        OpenChip(
                            label = label,
                            isOn = dark == v,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = { prefs.setDarkMode(v) },
                        )
                    }
                }
            }
        }
        OpenCard(modifier = Modifier.testTag("appearance_refresh")) {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Screen refresh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecUi.charcoal,
                    softWrap = true,
                )
                Text(
                    "Prefer 60 / 90 / 120 / 144 Hz when the phone supports it. Device may clamp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted,
                    softWrap = true,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    hzChoices.forEach { hz ->
                        OpenChip(
                            label = "${hz}Hz",
                            isOn = refreshHz == hz,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                refreshHz = hz
                                prefs.refreshHz = hz
                                (context as? android.app.Activity)?.let {
                                    DisplayRefreshController.apply(it, hz)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
