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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
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

private data class DrawerItem(val route: AppRoute, val label: String, val icon: ImageVector)

private val allDrawer = listOf(
    DrawerItem(AppRoute.Home, "Home", Icons.Default.Home),
    DrawerItem(AppRoute.History, "History", Icons.Default.History),
    DrawerItem(AppRoute.Dictionary, "Dictionary", Icons.Default.Book),
    DrawerItem(AppRoute.Snippets, "Snippets", Icons.AutoMirrored.Filled.ShortText),
    DrawerItem(AppRoute.Style, "Style", Icons.Default.Style),
    DrawerItem(AppRoute.Settings, "Settings", Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    route: AppRoute,
    onNavigate: (AppRoute) -> Unit,
    isItemVisible: (AppRoute) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val title = when (route) {
        AppRoute.Appearance -> "Appearance"
        AppRoute.BubbleSettings -> "Bubble"
        AppRoute.HomeModules -> "Home layout"
        AppRoute.NavModules -> "Menu items"
        else -> route.title
    }
    val drawerItems = allDrawer.filter { item ->
        item.route == AppRoute.Home || item.route == AppRoute.Settings || isItemVisible(item.route)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Open Flow",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Local · calm · private",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 28.dp).padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                drawerItems.forEach { item ->
                    val selected = when (route) {
                        AppRoute.Appearance, AppRoute.BubbleSettings,
                        AppRoute.HomeModules, AppRoute.NavModules ->
                            item.route == AppRoute.Settings
                        else -> item.route == route
                    }
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            onNavigate(item.route)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(item.icon, contentDescription = item.label)
                        },
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
            containerColor = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
