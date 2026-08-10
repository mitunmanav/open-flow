package app.openflow.ui.shell

enum class AppRoute(val title: String, val navId: String? = null) {
    Home("Open Flow", "home"),
    Dictionary("Dictionary", "dictionary"),
    Snippets("Snippets", "snippets"),
    Style("Style", "style"),
    History("History", "history"),
    Settings("Settings", "settings"),
    Customize("Customize", "customize"),
    Appearance("Appearance"),
    BubbleSettings("Bubble"),
    HomeModules("Home layout"),
    NavModules("Menu items"),
    Cleanup("Cleanup"),
    Privacy("History & privacy"),
    Sounds("Sounds & haptics"),
}

/** Bottom bar primary destinations — no Settings, no History. */
val BottomBarRoutes = listOf(
    AppRoute.Home,
    AppRoute.Dictionary,
    AppRoute.Snippets,
    AppRoute.Style,
)

fun AppRoute.isBottomBar(): Boolean = this in BottomBarRoutes ||
    this == AppRoute.Home || this == AppRoute.Dictionary ||
    this == AppRoute.Snippets || this == AppRoute.Style
