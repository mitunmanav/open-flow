package app.openflow.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class NavItem(val route: AppRoute, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    NavItem(AppRoute.Home, "Home", Icons.Default.Home),
    NavItem(AppRoute.History, "History", Icons.Default.History),
    NavItem(AppRoute.Settings, "Settings", Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    route: AppRoute,
    onNavigate: (AppRoute) -> Unit,
    isDrawerExtraVisible: (AppRoute) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit
) {
    // drawer extras unused in sleek shell; keep param for call-site compat
    @Suppress("UNUSED_PARAMETER")
    val _drawer = isDrawerExtraVisible

    val title = when (route) {
        AppRoute.Appearance -> "Appearance"
        AppRoute.BubbleSettings -> "Bubble"
        AppRoute.HomeModules -> "Home layout"
        AppRoute.NavModules -> "Menu"
        AppRoute.Cleanup -> "Cleanup"
        AppRoute.Privacy -> "Privacy"
        AppRoute.Sounds -> "Sounds"
        AppRoute.Dictionary -> "Dictionary"
        AppRoute.Snippets -> "Snippets"
        AppRoute.Style -> "Style"
        AppRoute.Customize -> "Customize"
        else -> route.title
    }

    val showBottom = route.isBottomBar() ||
        route == AppRoute.Home ||
        route == AppRoute.History ||
        route == AppRoute.Settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (showBottom) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEach { item ->
                        val selected = when {
                            route == item.route -> true
                            item.route == AppRoute.Settings && route in listOf(
                                AppRoute.Settings, AppRoute.Appearance, AppRoute.BubbleSettings,
                                AppRoute.Cleanup, AppRoute.Privacy, AppRoute.Sounds,
                                AppRoute.Dictionary, AppRoute.Snippets, AppRoute.Style,
                                AppRoute.Customize, AppRoute.HomeModules, AppRoute.NavModules
                            ) -> true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}
