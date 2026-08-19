package app.openflow.ui.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.prefs.FlowPrefs
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.OnDeviceSpeechPolicy
import app.openflow.stt.SttTuning
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.theme.SecUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeechDeviceSettings(prefs: FlowPrefs) {
    var sttProfile by remember { mutableStateOf(prefs.sttProfile) }
    var preferOnDevice by remember { mutableStateOf(prefs.preferOnDevice) }
    var languageTag by remember { mutableStateOf(prefs.languageTag) }

    OpenCard {
        Column(
            Modifier.padding(Dimen.MIN_PADDING).wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
        ) {
            Text(
                "Dictation speed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SecUi.charcoal,
                softWrap = true,
            )
            Text(
                "Fast = shorter silence wait. Accurate = longer listen.",
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                listOf(
                    SttTuning.PROFILE_FAST to "Fast",
                    SttTuning.PROFILE_BALANCED to "Balanced",
                    SttTuning.PROFILE_ACCURATE to "Accurate",
                ).forEach { (id, label) ->
                    OpenChip(
                        label = label,
                        isOn = sttProfile == id,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            sttProfile = id
                            prefs.sttProfile = id
                            FlowAccessibilityService.instance?.applyPrefsVisual()
                        },
                    )
                }
            }
        }
    }

    OpenCard {
        Column(
            Modifier.padding(Dimen.MIN_PADDING).wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
        ) {
            Text(
                "On-device speech",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SecUi.charcoal,
                softWrap = true,
            )
            Text(
                OnDeviceSpeechPolicy.honesty(preferOnDevice),
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
                modifier = Modifier.testTag("on_device_honesty"),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                OpenChip(
                    label = "Off",
                    isOn = !preferOnDevice,
                    modifier = Modifier.wrapContentHeight().testTag("on_device_off"),
                    onClick = {
                        preferOnDevice = false
                        prefs.preferOnDevice = false
                    },
                )
                OpenChip(
                    label = "On",
                    isOn = preferOnDevice,
                    modifier = Modifier.wrapContentHeight().testTag("on_device_on"),
                    onClick = {
                        preferOnDevice = true
                        prefs.preferOnDevice = true
                    },
                )
            }
        }
    }

    OpenCard {
        Column(
            Modifier.padding(Dimen.MIN_PADDING).wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
        ) {
            Text(
                "Speech language",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SecUi.charcoal,
                softWrap = true,
            )
            Text(
                "Used by system STT and cloud ears.",
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                LanguagePolicy.SUPPORTED_LANGUAGES.forEach { opt ->
                    OpenChip(
                        label = opt.displayName,
                        isOn = languageTag == opt.tag,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            languageTag = opt.tag
                            prefs.languageTag = opt.tag
                        },
                    )
                }
            }
        }
    }
}
