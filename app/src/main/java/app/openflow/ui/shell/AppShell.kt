package app.openflow.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class NavItem(val route: AppRoute, val label: String, val icon: ImageVector)

/** Bottom: primary only. Style lives here (not drawer). */
private val bottomItems = listOf(
    NavItem(AppRoute.Home, "Home", Icons.Default.Home),
    NavItem(AppRoute.Dictionary, "Dict", Icons.Default.Book),
    NavItem(AppRoute.Snippets, "Snips", Icons.AutoMirrored.Filled.ShortText),
    NavItem(AppRoute.Style, "Style", Icons.Default.Style),
)

/** Drawer: Settings + extras — never Home/Dict/Snips/Style. */
private val drawerExtras = listOf(
    NavItem(AppRoute.Settings, "Settings", Icons.Default.Settings),
    NavItem(AppRoute.History, "History", Icons.Default.History),
    NavItem(AppRoute.Customize, "Customize", Icons.Default.Tune),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    route: AppRoute,
    onNavigate: (AppRoute) -> Unit,
    isDrawerExtraVisible: (AppRoute) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val title = when (route) {
        AppRoute.Appearance -> "Appearance"
        AppRoute.BubbleSettings -> "Bubble"
        AppRoute.HomeModules -> "Home layout"
        AppRoute.NavModules -> "Menu items"
        AppRoute.Cleanup -> "Cleanup"
        AppRoute.Privacy -> "History & privacy"
        AppRoute.Sounds -> "Sounds & haptics"
        else -> route.title
    }

    val drawerItems = drawerExtras.filter { item ->
        item.route == AppRoute.Settings || isDrawerExtraVisible(item.route)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Open Flow",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Extras only · no bottom duplicates",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 28.dp).padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                drawerItems.forEach { item ->
                    val selected = when {
                        route == item.route -> true
                        item.route == AppRoute.Settings && route in listOf(
                            AppRoute.Appearance, AppRoute.BubbleSettings,
                            AppRoute.Cleanup, AppRoute.Privacy, AppRoute.Sounds
                        ) -> true
                        item.route == AppRoute.Customize && route in listOf(
                            AppRoute.Customize, AppRoute.HomeModules, AppRoute.NavModules
                        ) -> true
                        else -> false
                    }
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            onNavigate(item.route)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
