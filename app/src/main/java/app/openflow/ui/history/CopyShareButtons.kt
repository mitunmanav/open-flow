package app.openflow.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton

@Composable
fun CopyShareButtons(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
    ) {
        OpenButton(
            text = "Copy",
            onClick = onCopy,
            fill = false,
            variant = ButtonVariant.Outlined,
            modifier = Modifier.testTag("history_copy"),
        )
        OpenButton(
            text = "Share",
            onClick = onShare,
            fill = false,
            variant = ButtonVariant.Outlined,
            modifier = Modifier.testTag("history_share"),
        )
    }
}
