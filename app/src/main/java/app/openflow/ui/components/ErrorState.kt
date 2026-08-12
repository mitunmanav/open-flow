package app.openflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen

/** Error panel — hard charcoal title + error accent, cream surface context. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.PAGE_PAD + Dimen.GAP)
            .semantics(mergeDescendants = true) {
                contentDescription = "Error: $message"
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = scheme.error
        )
        Spacer(Modifier.height(Dimen.MIN_PADDING))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = scheme.onSurface,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(Modifier.height(Dimen.GAP))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = scheme.error,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        if (onRetry != null) {
            Spacer(Modifier.height(Dimen.MIN_PADDING))
            OpenButton(
                text = "Retry",
                onClick = onRetry,
                variant = ButtonVariant.Outlined,
                contentDescription = "Retry after error"
            )
        }
    }
}
