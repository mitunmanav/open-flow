package app.openflow.ui.walkthrough

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
fun WalkthroughPager(
    page: WalkthroughPolicy.Page,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body) = when (page) {
        WalkthroughPolicy.Page.WHAT -> "What" to
            "Open Flow types what you say. Not a keyboard. Keep yours. English only."
        WalkthroughPolicy.Page.TALK -> "Talk" to
            "Tap bubble → speak → tap again. X throws away. Hold = talk while holding."
        WalkthroughPolicy.Page.DICT_VS_SNIP -> "Dict vs snippet" to
            "Dictionary changes one word. Snippet pastes a whole block."
        WalkthroughPolicy.Page.PRIVACY -> "Privacy" to
            "We do not upload. Phone speech may still use Google. History stays on this phone. You can wipe or never save."
        WalkthroughPolicy.Page.READY -> "Ready" to
            "Focus a text field. Tap the bubble. You are ready."
    }
    val nextLabel = if (page == WalkthroughPolicy.Page.READY) "Done" else "Next"

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .testTag("walkthrough"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        OpenCard {
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
                OpenButton(
                    text = nextLabel,
                    onClick = onNext,
                    modifier = Modifier.testTag("walkthrough_next"),
                )
                OpenButton(
                    text = "Skip",
                    onClick = onSkip,
                    variant = ButtonVariant.Outlined,
                    modifier = Modifier.testTag("walkthrough_skip"),
                )
            }
        }
    }
}
