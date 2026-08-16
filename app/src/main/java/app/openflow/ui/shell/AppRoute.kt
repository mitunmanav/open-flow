package app.openflow.ui.shell

enum class AppRoute(val title: String, val navId: String? = null) {
    Home("Open Flow", "home"),
    History("History", "history"),
    Dictionary("Dictionary", "dictionary"),
    Snippets("Snippets", "snippets"),
    Style("Style", "style"),
    Insights("Insights", "insights"),
    Settings("Settings", "settings"),
    SpeechAi("Speech + AI"),
    Appearance("Appearance"),
    BubbleSettings("Bubble"),
    HomeModules("Home layout"),
    Cleanup("Cleanup"),
    Privacy("Privacy"),
    Sounds("Sounds"),
    Setup("Set up", "setup"),
}

/**
 * Hub tabs: Home · Dictionary · Snippets · Style · Insights.
 * History lives on Home. Settings is the gear, not a tab.
 */
val BottomBarRoutes = listOf(
    AppRoute.Home,
    AppRoute.Dictionary,
    AppRoute.Snippets,
    AppRoute.Style,
    AppRoute.Insights,
)

/**
 * Settings hub + settings children. Dict/Snips/Style are tabs, not settings.
 */
val SettingsSubtreeRoutes = setOf(
    AppRoute.Settings,
    AppRoute.SpeechAi,
    AppRoute.Appearance,
    AppRoute.BubbleSettings,
    AppRoute.Cleanup,
    AppRoute.Privacy,
    AppRoute.Sounds,
    AppRoute.HomeModules,
)

fun AppRoute.isBottomBar(): Boolean = this in BottomBarRoutes

fun AppRoute.isSettingsSubtree(): Boolean = this in SettingsSubtreeRoutes

/** Toolbar / system Back target. Root tabs return self (Back disabled there). */
fun AppRoute.backTarget(): AppRoute = when {
    isBottomBar() -> this
    this == AppRoute.Settings -> AppRoute.Home
    isSettingsSubtree() -> AppRoute.Settings
    else -> AppRoute.Home
}
