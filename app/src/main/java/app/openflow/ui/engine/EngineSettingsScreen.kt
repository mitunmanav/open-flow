package app.openflow.ui.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenDropdown
import app.openflow.ui.components.OpenTextField

@Composable
fun EngineSettingsScreen(
    initialEar: String = "system",
    initialBrain: String = "none",
    initialUrl: String = "",
    initialSarvamMode: String = "transcribe",
    initialKeyMask: String = "",
    onPick: (ear: String, brain: String) -> Unit = { _, _ -> },
    onSaveKey: (String) -> Unit = {},
    onSaveUrl: (String) -> Unit = {},
    onSarvamMode: (String) -> Unit = {},
    onKeyMask: () -> String = { "" },
) {
    var ear by remember { mutableStateOf(initialEar) }
    var brain by remember { mutableStateOf(initialBrain) }
    var url by remember { mutableStateOf(initialUrl) }
    var sarvamMode by remember { mutableStateOf(initialSarvamMode) }
    var keyDraft by remember { mutableStateOf("") }
    var savedMask by remember { mutableStateOf(initialKeyMask) }
    val state = EnginePickerState.of(ear, brain)
    val keyWarn = EnginePickerState.missingKeyLine(state.needsKey, savedMask)
    val scheme = MaterialTheme.colorScheme
    val earOptions = remember { EnginePickerVisibility.visibleEars() }
    val brainOptions = remember(url) { EnginePickerVisibility.visibleBrains(url) }
    val earGroup = remember {
        EnginePickerVisibility.visibleEarSections()
            .flatMap { sec -> sec.items.map { it.id to sec.title } }
            .toMap()
    }
    val brainGroup = remember(url) {
        EnginePickerVisibility.visibleBrainSections(url)
            .flatMap { sec -> sec.items.map { it.id to sec.title } }
            .toMap()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState())
            .testTag("engine_settings"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        OpenCard(modifier = Modifier.testTag("engine_honesty_card")) {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    text = state.honesty,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.testTag("engine_honesty")
                )
                Text(
                    text = stringResource(R.string.privacy_no_internet),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.testTag("engine_internet_honesty")
                )
            }
        }

        OpenDropdown(
            label = stringResource(R.string.speech_ai_ear),
            selectedId = ear,
            options = earOptions,
            enabled = { EnginePickerVisibility.showEar(it) },
            groupOf = { earGroup[it] },
            testTag = "ear_dropdown",
            onSelect = {
                ear = it
                onPick(it, brain)
                savedMask = onKeyMask()
            }
        )

        EnginePickerState.earDisabledReason(ear)?.let { why ->
            Text(
                text = why,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.testTag("engine_ear_disabled")
            )
        }

        OpenDropdown(
            label = stringResource(R.string.speech_ai_brain),
            selectedId = brain,
            options = brainOptions,
            enabled = { EnginePickerVisibility.showBrain(it, url) },
            groupOf = { brainGroup[it] },
            testTag = "brain_dropdown",
            onSelect = {
                brain = it
                onPick(ear, it)
                savedMask = onKeyMask()
            }
        )
        EnginePickerState.brainDisabledReason(brain, url)?.let { why ->
            Text(
                text = why,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.testTag("engine_brain_disabled")
            )
        }

        if (state.showSarvamMode) {
            OpenDropdown(
                label = stringResource(R.string.speech_ai_sarvam_mode),
                selectedId = sarvamMode,
                options = EnginePickerState.sarvamModes,
                testTag = "sarvam_dropdown",
                onSelect = {
                    sarvamMode = it
                    onSarvamMode(it)
                }
            )
        }

        val lit = state.chips.filter { it.lit }
        if (lit.isNotEmpty()) {
            Text(
                text = "On: " + lit.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.testTag("engine_feature_chips")
            )
        }

        if (state.needsKey) {
            OpenCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    if (keyWarn != null) {
                        Text(
                            text = keyWarn,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.error,
                            modifier = Modifier.testTag("engine_key_needed")
                        )
                    }
                    OpenTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = stringResource(R.string.speech_ai_key),
                        placeholder = savedMask.ifEmpty {
                            stringResource(R.string.speech_ai_key_hint)
                        },
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
                                onSaveKey(typed)
                                savedMask = onKeyMask().ifEmpty {
                                    EnginePickerState.maskKey(typed)
                                }
                                keyDraft = ""
                            }
                        },
                        enabled = keyDraft.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engine_save_key")
                    )
                }
            }
        }

        OpenTextField(
            value = url,
            onValueChange = {
                url = it
                onSaveUrl(it)
            },
            label = stringResource(R.string.speech_ai_url),
            placeholder = stringResource(R.string.speech_ai_url_hint),
            contentDescription = stringResource(R.string.speech_ai_url),
            modifier = Modifier.testTag("engine_url")
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}
