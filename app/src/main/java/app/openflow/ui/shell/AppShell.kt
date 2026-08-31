package app.openflow.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import app.openflow.ui.a11y.OpenIcons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.ui.HapticPick
import app.openflow.ui.LocalHapticTap
import app.openflow.ui.UiHapticMap
import app.openflow.ui.a11y.Dimen

/**
 * Bottom-tab item. [label] may be short for bar width;
 * [contentDescription] stays full for a11y.
 */
private data class NavItem(
    val route: AppRoute,
    val label: String,
    val contentDescription: String,
    val icon: ImageVector,
    val testTag: String,
)

/** Wispr Android tabs: Home · Dictionary · Snippets · Style. */
private val bottomItems = listOf(
    NavItem(AppRoute.Home, NavBarTokens.shortLabel(AppRoute.Home), OpenIcons.HomeDesc, OpenIcons.Home, "nav_home"),
    NavItem(AppRoute.Dictionary, NavBarTokens.shortLabel(AppRoute.Dictionary), OpenIcons.BookDesc, OpenIcons.Book, "nav_dictionary"),
    NavItem(AppRoute.Snippets, NavBarTokens.shortLabel(AppRoute.Snippets), OpenIcons.ShortTextDesc, OpenIcons.ShortText, "nav_snippets"),
    NavItem(AppRoute.Style, NavBarTokens.shortLabel(AppRoute.Style), OpenIcons.StyleDesc, OpenIcons.Style, "nav_style"),
    NavItem(AppRoute.Insights, NavBarTokens.shortLabel(AppRoute.Insights), OpenIcons.InsightsDesc, OpenIcons.Insights, "nav_insights"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    route: AppRoute,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit = { onNavigate(route.backTarget()) },
    content: @Composable (PaddingValues) -> Unit
) {
    val title = route.title
    val showBack = !route.isBottomBar() && route != AppRoute.Setup
    val showSettingsGear = route.isBottomBar()

    // Theme-aware shell (light + dark readable)
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.background
    val onSurface = scheme.onBackground
    val muted = scheme.onSurfaceVariant
    val selectedBg = scheme.primary
    val onSelected = scheme.onPrimary
    val hardShape = RectangleShape
    val view = LocalView.current
    val hapticOn = HapticPick.constant(LocalHapticTap.current) != null

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
                                .sizeIn(
                                    minWidth = NavBarTokens.iconMin,
                                    minHeight = NavBarTokens.iconMin
                                )
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
                actions = {
                    if (showSettingsGear) {
                        IconButton(
                            onClick = { onNavigate(AppRoute.Settings) },
                            modifier = Modifier
                                .sizeIn(
                                    minWidth = NavBarTokens.iconMin,
                                    minHeight = NavBarTokens.iconMin
                                )
                                .testTag("nav_settings")
                        ) {
                            Icon(
                                OpenIcons.Settings,
                                contentDescription = OpenIcons.SettingsDesc,
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
            if (route != AppRoute.Setup) {
                // No NavigationBarItem — M3 indicator is CircleShape stadium. Custom hard rect.
                Surface(
                    color = surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    shape = RectangleShape,
                    modifier = Modifier
                        .testTag("nav_bar")
                        .drawBehind {
                            drawLine(
                                color = scheme.outline,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = Dimen.BORDER.toPx()
                            )
                        }
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                    ) {
                        bottomItems.forEach { item ->
                            val selected = route == item.route
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(
                                        minWidth = NavBarTokens.tabMin,
                                        minHeight = NavBarTokens.tabMin
                                    )
                                    .selectable(
                                        selected = selected,
                                        role = Role.Tab,
                                        onClick = {
                                            if (hapticOn) {
                                                view.performHapticFeedback(
                                                    UiHapticMap.constant(UiHapticMap.Event.NAV_TAB)
                                                )
                                            }
                                            onNavigate(item.route)
                                        }
                                    )
                                    .testTag(item.testTag)
                                    .padding(vertical = NavBarTokens.tabPadV),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) selectedBg else Color.Transparent,
                                            shape = hardShape
                                        )
                                        .then(
                                            if (selected) {
                                                Modifier.border(
                                                    Dimen.BORDER,
                                                    scheme.outline,
                                                    hardShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(
                                            horizontal = NavBarTokens.selectedPadH,
                                            vertical = NavBarTokens.selectedPadV
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.contentDescription,
                                        tint = if (selected) onSelected else muted
                                    )
                                }
                                Text(
                                    item.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) onSurface else muted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = surface,
        content = content
    )
}
