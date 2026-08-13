package app.openflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen

/** Empty panel — theme-aware onSurface colors. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val a11y = if (subtitle.isNullOrBlank()) title else "$title. $subtitle"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { clip = false }
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.PAGE_PAD + Dimen.GAP)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = scheme.onSurface
        )
        Spacer(Modifier.height(Dimen.MIN_PADDING))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = scheme.onSurface,
            overflow = TextOverflow.Visible,
            softWrap = true,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(Dimen.GAP))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant,
                overflow = TextOverflow.Visible,
                softWrap = true,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }
    }
}
