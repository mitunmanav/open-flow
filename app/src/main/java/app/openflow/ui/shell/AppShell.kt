package app.openflow.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class NavItem(val route: AppRoute, val label: String, val icon: ImageVector)

/** Always-visible primary tabs. */
private val bottomItems = listOf(
    NavItem(AppRoute.Home, "Home", Icons.Default.Home),
    NavItem(AppRoute.History, "History", Icons.Default.History),
    NavItem(AppRoute.Dictionary, "Dict", Icons.Default.Book),
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
    @Suppress("UNUSED_PARAMETER")
    val unused = isDrawerExtraVisible

    val title = when (route) {
        AppRoute.Home -> "Open Flow"
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
        AppRoute.History -> "History"
        AppRoute.Settings -> "Settings"
    }

    // Sub-screens need an explicit Back — never trap the user.
    val showBack = route !in listOf(
        AppRoute.Home,
        AppRoute.History,
        AppRoute.Dictionary,
        AppRoute.Settings
    )
    val backTarget = when (route) {
        AppRoute.Snippets, AppRoute.Style, AppRoute.Appearance,
        AppRoute.BubbleSettings, AppRoute.Cleanup, AppRoute.Privacy,
        AppRoute.Sounds, AppRoute.Customize, AppRoute.HomeModules,
        AppRoute.NavModules -> AppRoute.Settings
        else -> AppRoute.Home
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { onNavigate(backTarget) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
            // ALWAYS show — previous bug hid nav on Settings children
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                bottomItems.forEach { item ->
                    val selected = when {
                        route == item.route -> true
                        item.route == AppRoute.Settings && route in listOf(
                            AppRoute.Settings,
                            AppRoute.Appearance,
                            AppRoute.BubbleSettings,
                            AppRoute.Cleanup,
                            AppRoute.Privacy,
                            AppRoute.Sounds,
                            AppRoute.Snippets,
                            AppRoute.Style,
                            AppRoute.Customize,
                            AppRoute.HomeModules,
                            AppRoute.NavModules
                        ) -> true
                        else -> false
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.testTag("nav_" + item.label.lowercase()),
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}
