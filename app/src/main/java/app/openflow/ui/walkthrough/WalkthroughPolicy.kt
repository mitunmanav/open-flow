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
            "Not a new keyboard. Yours stays. English for now.",
        )
        Page.TALK -> PageCopy(
            "How to talk",
            "Tap bubble, speak, tap again. X drops it. Hold to talk.",
        )
        Page.DICT_VS_SNIP -> PageCopy(
            "Fixes vs shortcuts",
            "Dictionary fixes one word. Snippets paste a whole line.",
        )
        Page.PRIVACY -> PageCopy(
            "Privacy",
            PrivacyHonesty.WALKTHROUGH,
        )
        Page.READY -> PageCopy(
            "Try it",
            "Open a text box. Tap the bubble. That's it.",
        )
    }

    fun a11yLabel(page: Page): String =
        "${progressLabel(page)}. ${copy(page).title}."
}
