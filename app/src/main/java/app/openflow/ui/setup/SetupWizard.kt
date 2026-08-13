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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val copy = FirstRunPolicy.copy(step)
    val onPrimary = when (step) {
        FirstRunPolicy.Step.A11Y -> onEnableBubble
        FirstRunPolicy.Step.MIC -> onMic
        FirstRunPolicy.Step.BATTERY -> onBattery
        FirstRunPolicy.Step.DONE -> ({})
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .testTag("setup_wizard")
            .semantics { contentDescription = FirstRunPolicy.a11yLabel(step) },
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        if (step != FirstRunPolicy.Step.DONE) {
            Text(
                text = FirstRunPolicy.progressLabel(step),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("setup_progress"),
            )
            SetupStepCard(
                title = copy.title,
                body = copy.body,
                primary = copy.primary,
                onPrimary = onPrimary,
                secondary = copy.secondary,
                onSecondary = if (copy.secondary != null) onSkipBattery else null,
                stepTag = stepTag,
            )
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
                    contentDescription = "Skip battery optimization",
                    modifier = Modifier.testTag("setup_skip_battery"),
                )
            }
        }
    }
}
