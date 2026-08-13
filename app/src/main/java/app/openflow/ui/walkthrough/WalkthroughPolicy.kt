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
            "What",
            "Open Flow types what you say. Not a keyboard. Keep yours. English only.",
        )
        Page.TALK -> PageCopy(
            "Talk",
            "Tap bubble → speak → tap again. X throws away. Hold = talk while holding.",
        )
        Page.DICT_VS_SNIP -> PageCopy(
            "Dict vs snippet",
            "Dictionary changes one word. Snippet pastes a whole block.",
        )
        Page.PRIVACY -> PageCopy(
            "Privacy",
            PrivacyHonesty.WALKTHROUGH,
        )
        Page.READY -> PageCopy(
            "Ready",
            "Focus a text field. Tap the bubble. You are ready.",
        )
    }

    fun a11yLabel(page: Page): String =
        "${progressLabel(page)}. ${copy(page).title}."
}
