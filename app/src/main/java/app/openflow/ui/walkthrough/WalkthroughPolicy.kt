package app.openflow.ui.walkthrough

object WalkthroughPolicy {
    enum class Page { WHAT, TALK, DICT_VS_SNIP, PRIVACY, READY }

    fun pages(): List<Page> = Page.entries

    fun needsWalkthrough(seen: Boolean): Boolean = !seen
}
