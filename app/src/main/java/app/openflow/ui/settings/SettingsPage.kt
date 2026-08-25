package app.openflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.openflow.ui.a11y.Dimen

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    intro: String? = null,
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState())
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
    ) {
        if (!intro.isNullOrBlank()) {
            Text(
                intro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true,
            )
        }
        content()
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}
