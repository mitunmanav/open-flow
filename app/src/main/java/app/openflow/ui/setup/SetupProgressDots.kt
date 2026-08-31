package app.openflow.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SetupProgressDots(step: FirstRunPolicy.Step) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("setup_dots"),
    ) {
        repeat(FirstRunPolicy.totalSteps()) { i ->
            val filled = i < FirstRunPolicy.stepNumber(step)
            Box(
                Modifier
                    .padding(vertical = 4.dp)
                    .size(12.dp)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        CircleShape,
                    ),
            )
        }
    }
}
