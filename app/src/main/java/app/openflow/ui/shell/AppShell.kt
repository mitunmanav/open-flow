package app.openflow.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom-tab item. [label] may be short for bar width;
 * [contentDescription] stays full for a11y.
 */
private data class NavItem(
    val route: AppRoute,
    val label: String,
    val contentDescription: String,
    val icon: ImageVector,
) {
    val testTag: String
        get() = "nav_" + (route.navId ?: route.name.lowercase())
}

/** Always-visible primary tabs. Stable list — do not rebuild per frame. */
private val bottomItems = listOf(
    NavItem(AppRoute.Home, "Home", "Home", Icons.Default.Home),
    NavItem(AppRoute.History, "History", "History", Icons.Default.History),
    NavItem(AppRoute.Dictionary, "Dict", "Dictionary", Icons.Default.Book),
    NavItem(AppRoute.Settings, "Settings", "Settings", Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    route: AppRoute,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit = { onNavigate(route.backTarget()) },
    isDrawerExtraVisible: (AppRoute) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = isDrawerExtraVisible

    val title = route.title
    val showBack = !route.isBottomBar()
    val settingsSelected = remember(route) { route.isSettingsSubtree() }

    // Theme-aware shell (light + dark readable)
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.background
    val onSurface = scheme.onBackground
    val muted = scheme.onSurfaceVariant
    val selectedBg = scheme.primary
    val onSelected = scheme.onPrimary
    val hardShape = MaterialTheme.shapes.small

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        modifier = Modifier.testTag("shell_title")
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("nav_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = onSurface,
                    navigationIconContentColor = onSurface,
                    actionIconContentColor = onSurface,
                    scrolledContainerColor = surface
                ),
                windowInsets = TopAppBarDefaults.windowInsets,
                modifier = Modifier.drawBehind {
                    val y = size.height - 1.dp.toPx()
                    drawLine(
                        color = scheme.outline,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = surface,
                contentColor = onSurface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = scheme.outline,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            ) {
                bottomItems.forEach { item ->
                    val selected = when {
                        route == item.route -> true
                        item.route == AppRoute.Settings && settingsSelected -> true
                        else -> false
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.testTag(item.testTag),
                        icon = {
                            // M3 hardcodes CircleShape stadium; hide it, draw 2dp block.
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (selected) selectedBg else Color.Transparent,
                                        shape = hardShape
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.dp, scheme.outline, hardShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.contentDescription)
                            }
                        },
                        label = {
                            Text(
                                item.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = onSelected,
                            selectedTextColor = onSurface,
                            indicatorColor = surface,
                            unselectedIconColor = muted,
                            unselectedTextColor = muted
                        )
                    )
                }
            }
        },
        containerColor = surface,
        content = content
    )
}
