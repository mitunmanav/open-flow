package app.openflow.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard

@Composable
fun SetupWizard(
    step: FirstRunPolicy.Step,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit,
    onBattery: () -> Unit,
    onSkipBattery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepTag = when (step) {
        FirstRunPolicy.Step.A11Y -> "setup_step_a11y"
        FirstRunPolicy.Step.MIC -> "setup_step_mic"
        FirstRunPolicy.Step.BATTERY -> "setup_step_battery"
        FirstRunPolicy.Step.DONE -> null
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .testTag("setup_wizard"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        when (step) {
            FirstRunPolicy.Step.A11Y -> SetupStepCard(
                title = "Turn on the Flow Bubble",
                body = "Accessibility lets Open Flow insert text in any app. Keep your keyboard.",
                primary = "Open Accessibility",
                onPrimary = onEnableBubble,
                stepTag = stepTag,
            )
            FirstRunPolicy.Step.MIC -> SetupStepCard(
                title = "Allow the microphone",
                body = "On-device speech. Open Flow never uploads audio.",
                primary = "Allow microphone",
                onPrimary = onMic,
                stepTag = stepTag,
            )
            FirstRunPolicy.Step.BATTERY -> SetupStepCard(
                title = "Keep the bubble alive",
                body = "Optional. Stop the phone from killing Open Flow.",
                primary = "Battery settings",
                onPrimary = onBattery,
                secondary = "Skip",
                onSecondary = onSkipBattery,
                stepTag = stepTag,
            )
            FirstRunPolicy.Step.DONE -> Unit
        }
    }
}

@Composable
private fun SetupStepCard(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    stepTag: String? = null,
) {
    OpenCard(modifier = if (stepTag != null) Modifier.testTag(stepTag) else Modifier) {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenButton(text = primary, onClick = onPrimary)
            if (secondary != null && onSecondary != null) {
                OpenButton(
                    text = secondary,
                    onClick = onSecondary,
                    variant = ButtonVariant.Outlined,
                )
            }
        }
    }
}
