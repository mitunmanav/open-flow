package app.openflow.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object SecUi {
    val cream: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val charcoal: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
    val stone: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val ink: Color
        @Composable get() = MaterialTheme.colorScheme.secondary
    val error: Color
        @Composable get() = MaterialTheme.colorScheme.error
    val muted: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val hardBorder: BorderStroke
        @Composable get() = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    val thinBorder: BorderStroke
        @Composable get() = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
}
