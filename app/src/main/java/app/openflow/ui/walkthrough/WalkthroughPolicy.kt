package app.openflow.ui.walkthrough

import app.openflow.ui.privacy.PrivacyHonesty

object WalkthroughPolicy {
    enum class Page { WHAT, TALK, DICT_VS_SNIP, PRIVACY, READY }

    data class PageCopy(val title: String, val body: String)

    fun pages(): List<Page> = Page.entries

    fun needsWalkthrough(seen: Boolean): Boolean = !seen

    fun totalPages(): Int = Page.entries.size

    fun pageNumber(page: Page): Int = pages().indexOf(page) + 1

    fun progressLabel(page: Page): String =
        "Page ${pageNumber(page)} of ${totalPages()}"

    fun nextLabel(page: Page): String =
        if (page == Page.READY) "Done" else "Next"

    fun copy(page: Page): PageCopy = when (page) {
        Page.WHAT -> PageCopy(
            "Speak to type",
            "Keep your keyboard. Tap the bubble to speak.",
        )
        Page.TALK -> PageCopy(
            "How to talk",
            "Tap the bubble to speak. Tap Done to insert. Tap Cancel to throw away. Hold to talk.",
        )
        Page.DICT_VS_SNIP -> PageCopy(
            "Fixes vs shortcuts",
            "A dictionary entry is one word. A snippet is a whole block.",
        )
        Page.PRIVACY -> PageCopy(
            "Privacy",
            PrivacyHonesty.WALKTHROUGH,
        )
        Page.READY -> PageCopy(
            "Try it",
            "Open a text box. Tap the bubble.",
        )
    }

    fun a11yLabel(page: Page): String =
        "${progressLabel(page)}. ${copy(page).title}."
}
