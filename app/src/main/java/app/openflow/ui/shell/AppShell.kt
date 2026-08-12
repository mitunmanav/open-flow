package app.openflow.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.ui.theme.BrutalColors

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
    isDrawerExtraVisible: (AppRoute) -> Boolean = { true },
    content: @Composable (PaddingValues) -> Unit
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = isDrawerExtraVisible

    val title = route.title
    val showBack = !route.isBottomBar()
    val backTarget = remember(route) { route.backTarget() }
    val settingsSelected = remember(route) { route.isSettingsSubtree() }

    // Light brutal shell tokens (not M3 tonal purple).
    val cream = BrutalColors.Cream
    val charcoal = BrutalColors.Charcoal
    val onCharcoal = BrutalColors.OnCharcoal
    val mutedInk = BrutalColors.Charcoal.copy(alpha = 0.55f)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = charcoal,
                        modifier = Modifier.testTag("shell_title")
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(
                            onClick = { onNavigate(backTarget) },
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("nav_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = charcoal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cream,
                    titleContentColor = charcoal,
                    navigationIconContentColor = charcoal,
                    actionIconContentColor = charcoal,
                    scrolledContainerColor = cream
                ),
                // Edge-to-edge: pad for status bar / cutout.
                windowInsets = TopAppBarDefaults.windowInsets,
                modifier = Modifier.drawBehind {
                    // Hard bottom rule (brutal, not soft elevation).
                    val y = size.height - 1.dp.toPx()
                    drawLine(
                        color = charcoal,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            // ALWAYS show — previous bug hid nav on Settings children
            NavigationBar(
                containerColor = cream,
                contentColor = charcoal,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.drawBehind {
                    // Hard top rule under content.
                    drawLine(
                        color = charcoal,
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
                            Icon(item.icon, contentDescription = item.contentDescription)
                        },
                        label = {
                            Text(
                                item.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        // Hard selected: charcoal pill + cream icon (not soft primaryContainer).
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = onCharcoal,
                            selectedTextColor = charcoal,
                            indicatorColor = charcoal,
                            unselectedIconColor = mutedInk,
                            unselectedTextColor = mutedInk
                        )
                    )
                }
            }
        },
        containerColor = cream,
        content = content
    )
}
