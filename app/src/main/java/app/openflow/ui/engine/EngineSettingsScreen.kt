package app.openflow.ui.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import app.openflow.R
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EngineSettingsScreen(
    initialEar: String = "system",
    initialBrain: String = "none",
    initialUrl: String = "",
    initialSarvamMode: String = "transcribe",
    onPick: (ear: String, brain: String) -> Unit = { _, _ -> },
    onSaveKey: (String) -> Unit = {},
    onSaveUrl: (String) -> Unit = {},
    onSarvamMode: (String) -> Unit = {},
) {
    var ear by remember { mutableStateOf(initialEar) }
    var brain by remember { mutableStateOf(initialBrain) }
    var url by remember { mutableStateOf(initialUrl) }
    var sarvamMode by remember { mutableStateOf(initialSarvamMode) }
    var keyDraft by remember { mutableStateOf("") }
    var savedMask by remember { mutableStateOf("") }
    val state = EnginePickerState.of(ear, brain)
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState())
            .testTag("engine_settings"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            text = state.honesty,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            modifier = Modifier.testTag("engine_honesty")
        )
        Text(
            text = stringResource(R.string.speech_ai_honesty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Text(
            text = stringResource(R.string.speech_ai_ear),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            EnginePickerState.ears.forEach { preset ->
                OpenChip(
                    label = preset.label,
                    isOn = ear == preset.id,
                    modifier = Modifier.testTag("ear_" + preset.id),
                    onClick = {
                        ear = preset.id
                        onPick(preset.id, brain)
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.speech_ai_brain),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            EnginePickerState.brains.forEach { preset ->
                OpenChip(
                    label = preset.label,
                    isOn = brain == preset.id,
                    modifier = Modifier.testTag("brain_" + preset.id),
                    onClick = {
                        brain = preset.id
                        onPick(ear, preset.id)
                    }
                )
            }
        }

        if (state.showSarvamMode) {
            Text(
                text = stringResource(R.string.speech_ai_sarvam_mode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                EnginePickerState.sarvamModes.forEach { preset ->
                    OpenChip(
                        label = preset.label,
                        isOn = sarvamMode == preset.id,
                        modifier = Modifier.testTag("sarvam_" + preset.id),
                        onClick = {
                            sarvamMode = preset.id
                            onSarvamMode(preset.id)
                        }
                    )
                }
            }
        }

        if (state.needsKey) {
            OpenTextField(
                value = keyDraft,
                onValueChange = { keyDraft = it },
                label = stringResource(R.string.speech_ai_key),
                placeholder = savedMask.ifEmpty { stringResource(R.string.speech_ai_key_hint) },
                contentDescription = stringResource(R.string.speech_ai_key),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.testTag("engine_key")
            )
            if (savedMask.isNotEmpty() && keyDraft.isEmpty()) {
                Text(
                    text = stringResource(R.string.speech_ai_key_saved, savedMask),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.testTag("engine_key_mask")
                )
            }
            OpenButton(
                text = stringResource(R.string.speech_ai_save_key),
                onClick = {
                    val typed = keyDraft.trim()
                    if (typed.isNotEmpty()) {
                        savedMask = EnginePickerState.maskKey(typed)
                        onSaveKey(typed)
                        keyDraft = ""
                    }
                },
                enabled = keyDraft.isNotBlank(),
                modifier = Modifier.testTag("engine_save_key")
            )
        }

        if (state.needsUrl) {
            OpenTextField(
                value = url,
                onValueChange = {
                    url = it
                    onSaveUrl(it)
                },
                label = stringResource(R.string.speech_ai_url),
                placeholder = stringResource(R.string.speech_ai_url_hint),
                contentDescription = stringResource(R.string.speech_ai_url),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.testTag("engine_url")
            )
        }

        Text(
            text = state.highLabel,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.testTag("engine_high_label")
        )
        Text(
            text = state.commandWhy ?: stringResource(R.string.speech_ai_command_on),
            style = MaterialTheme.typography.bodySmall,
            color = if (state.commandMode) scheme.onSurfaceVariant else scheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.testTag("engine_command_why")
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}
