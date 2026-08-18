package app.openflow.ui.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.openflow.R
import app.openflow.orchestrate.AiWhen
import app.openflow.orchestrate.RouteMode
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenDropdown
import app.openflow.ui.components.OpenTextField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EngineSettingsScreen(
    initialEar: String = "system",
    initialBrain: String = "none",
    initialRouteMode: RouteMode = RouteMode.LOCAL_THEN_AI,
    initialAiWhen: AiWhen = AiWhen.EVERY,
    initialUrl: String = "",
    initialSarvamMode: String = "transcribe",
    initialKeyMask: String = "",
    initialEarKeyMask: String = "",
    initialBrainKeyMask: String = "",
    onPick: (ear: String, brain: String) -> Unit = { _, _ -> },
    onRouteMode: (RouteMode) -> Unit = {},
    onAiWhen: (AiWhen) -> Unit = {},
    onSaveKey: (String) -> Unit = {},
    onSaveEarKey: (String) -> Unit = onSaveKey,
    onSaveBrainKey: (String) -> Unit = onSaveKey,
    onSaveUrl: (String) -> Unit = {},
    onSarvamMode: (String) -> Unit = {},
    onKeyMask: () -> String = { "" },
    onEarKeyMask: () -> String = onKeyMask,
    onBrainKeyMask: () -> String = onKeyMask,
) {
    var ear by remember { mutableStateOf(initialEar) }
    var brain by remember { mutableStateOf(initialBrain) }
    var routeMode by remember { mutableStateOf(initialRouteMode) }
    var aiWhen by remember { mutableStateOf(initialAiWhen) }
    var url by remember { mutableStateOf(initialUrl) }
    var sarvamMode by remember { mutableStateOf(initialSarvamMode) }
    var earKeyDraft by remember { mutableStateOf("") }
    var brainKeyDraft by remember { mutableStateOf("") }
    var savedEarMask by remember { mutableStateOf(initialEarKeyMask.ifEmpty { initialKeyMask }) }
    var savedBrainMask by remember { mutableStateOf(initialBrainKeyMask.ifEmpty { initialKeyMask }) }
    val state = EnginePickerState.of(ear, brain, routeMode = routeMode)
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

        OpenCard(modifier = Modifier.fillMaxWidth().testTag("route_mode_card")) {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    text = "Rewrite path",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = "Speech stays your ear pick. Cloud rewrite only if a key is saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpenChip(
                        label = "Local only",
                        isOn = routeMode == RouteMode.LOCAL_ONLY,
                        modifier = Modifier.testTag("route_local_only"),
                        onClick = {
                            routeMode = RouteMode.LOCAL_ONLY
                            onRouteMode(RouteMode.LOCAL_ONLY)
                        }
                    )
                    OpenChip(
                        label = "Local then AI",
                        isOn = routeMode == RouteMode.LOCAL_THEN_AI,
                        modifier = Modifier.testTag("route_local_then_ai"),
                        onClick = {
                            routeMode = RouteMode.LOCAL_THEN_AI
                            onRouteMode(RouteMode.LOCAL_THEN_AI)
                        }
                    )
                    OpenChip(
                        label = "AI first",
                        isOn = routeMode == RouteMode.AI_FIRST,
                        modifier = Modifier.testTag("route_ai_first"),
                        onClick = {
                            routeMode = RouteMode.AI_FIRST
                            onRouteMode(RouteMode.AI_FIRST)
                        }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpenChip(
                        label = "Every",
                        isOn = aiWhen == AiWhen.EVERY,
                        enabled = routeMode == RouteMode.LOCAL_THEN_AI,
                        modifier = Modifier.testTag("ai_when_every"),
                        onClick = {
                            aiWhen = AiWhen.EVERY
                            onAiWhen(AiWhen.EVERY)
                        }
                    )
                    OpenChip(
                        label = "Miss only",
                        isOn = aiWhen == AiWhen.MISS_ONLY,
                        enabled = routeMode == RouteMode.LOCAL_THEN_AI,
                        modifier = Modifier.testTag("ai_when_miss"),
                        onClick = {
                            aiWhen = AiWhen.MISS_ONLY
                            onAiWhen(AiWhen.MISS_ONLY)
                        }
                    )
                }
            }
        }

        // --- SPEECH-TO-TEXT SECTION ---
        EnginePickerState.manualFallbackHint(state.autoRoute)?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.testTag("ear_override_hint")
            )
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
                savedEarMask = onEarKeyMask()
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

        if (state.needsEarKey) {
            OpenCard(modifier = Modifier.fillMaxWidth().testTag("engine_ear_key_card")) {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    if (savedEarMask.isEmpty()) {
                        Text(
                            text = "Add API key for $ear speech recognition.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.error,
                            modifier = Modifier.testTag("engine_key_needed")
                        )
                    }
                    OpenTextField(
                        value = earKeyDraft,
                        onValueChange = { earKeyDraft = it },
                        label = "$ear API Key",
                        placeholder = savedEarMask.ifEmpty { "Paste $ear key" },
                        contentDescription = "Speech API Key",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.testTag("engine_key")
                    )
                    if (savedEarMask.isNotEmpty() && earKeyDraft.isEmpty()) {
                        Text(
                            text = stringResource(R.string.speech_ai_key_saved, savedEarMask),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.testTag("engine_key_mask")
                        )
                    }
                    OpenButton(
                        text = "Save Speech Key",
                        onClick = {
                            val typed = earKeyDraft.trim()
                            if (typed.isNotEmpty()) {
                                onSaveEarKey(typed)
                                savedEarMask = onEarKeyMask().ifEmpty {
                                    EnginePickerState.maskKey(typed)
                                }
                                earKeyDraft = ""
                            }
                        },
                        enabled = earKeyDraft.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engine_save_key")
                    )
                }
            }
        }

        // --- BRAIN / FORMATTING SECTION ---
        EnginePickerState.manualFallbackHint(state.autoRoute)?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.testTag("brain_override_hint")
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
                savedBrainMask = onBrainKeyMask()
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

        if (state.needsBrainKey) {
            OpenCard(modifier = Modifier.fillMaxWidth().testTag("engine_brain_key_card")) {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    if (savedBrainMask.isEmpty()) {
                        Text(
                            text = "Add API key for $brain formatting model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.error
                        )
                    }
                    OpenTextField(
                        value = brainKeyDraft,
                        onValueChange = { brainKeyDraft = it },
                        label = "$brain API Key",
                        placeholder = savedBrainMask.ifEmpty { "Paste $brain key" },
                        contentDescription = "Brain API Key",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.testTag("engine_brain_key")
                    )
                    if (savedBrainMask.isNotEmpty() && brainKeyDraft.isEmpty()) {
                        Text(
                            text = stringResource(R.string.speech_ai_key_saved, savedBrainMask),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    OpenButton(
                        text = "Save Brain Key",
                        onClick = {
                            val typed = brainKeyDraft.trim()
                            if (typed.isNotEmpty()) {
                                onSaveBrainKey(typed)
                                savedBrainMask = onBrainKeyMask().ifEmpty {
                                    EnginePickerState.maskKey(typed)
                                }
                                brainKeyDraft = ""
                            }
                        },
                        enabled = brainKeyDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
