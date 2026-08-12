package app.openflow.ui.shell

enum class AppRoute(val title: String, val navId: String? = null) {
    Home("Open Flow", "home"),
    History("History", "history"),
    Dictionary("Dictionary", "dictionary"),
    Snippets("Snippets", "snippets"),
    Style("Style", "style"),
    Settings("Settings", "settings"),
    Customize("Customize", "customize"),
    Appearance("Appearance"),
    BubbleSettings("Bubble"),
    HomeModules("Home layout"),
    NavModules("Menu items"),
    Cleanup("Cleanup"),
    Privacy("Privacy"),
    Sounds("Sounds"),
}

/** Bottom bar primary destinations. */
val BottomBarRoutes = listOf(
    AppRoute.Home,
    AppRoute.History,
    AppRoute.Dictionary,
    AppRoute.Settings,
)

fun AppRoute.isBottomBar(): Boolean = this in BottomBarRoutes
