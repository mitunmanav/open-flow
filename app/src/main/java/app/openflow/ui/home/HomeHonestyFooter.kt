package app.openflow.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.openflow.ui.privacy.PrivacyHonesty

@Composable
fun HomeHonestyFooter(modifier: Modifier = Modifier) {
    Text(
        PrivacyHonesty.HOME_FOOTER,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        softWrap = true,
        modifier = modifier,
    )
}
