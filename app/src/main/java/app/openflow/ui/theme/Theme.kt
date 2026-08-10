package app.openflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF0F766E)
private val TealLight = Color(0xFF14B8A6)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = TealLight
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    secondary = Teal
)

@Composable
fun OpenFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
