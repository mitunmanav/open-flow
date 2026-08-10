package app.openflow.ui.shell

enum class AppRoute(val title: String, val navId: String? = null) {
    Home("Open Flow", "home"),
    History("History", "history"),
    Dictionary("Dictionary", "dictionary"),
    Snippets("Snippets", "snippets"),
    Style("Style", "style"),
    Settings("Settings", "settings"),
    Appearance("Appearance"),
    BubbleSettings("Bubble"),
    HomeModules("Home layout"),
    NavModules("Menu items"),
}
