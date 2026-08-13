package app.openflow.ui.walkthrough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
fun WalkthroughPager(
    page: WalkthroughPolicy.Page,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = WalkthroughPolicy.copy(page)
    val nextLabel = WalkthroughPolicy.nextLabel(page)

    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.safeDrawing))
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .testTag("walkthrough")
            .semantics { contentDescription = WalkthroughPolicy.a11yLabel(page) },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = WalkthroughPolicy.progressLabel(page),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = Dimen.GAP)
                .testTag("walkthrough_progress"),
        )
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
            ) {
                Text(
                    copy.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    copy.body,
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
