package app.openflow.ui.home

object HomeBannerPolicy {
    enum class Banner { REPAIR_A11Y, ALLOW_MIC, END_SNOOZE, NONE }

    data class BannerCopy(
        val title: String,
        val body: String?,
        val cta: String?,
        val a11yLabel: String,
    )

    fun banner(bubbleOn: Boolean, micOn: Boolean, snoozed: Boolean): Banner = when {
        !bubbleOn -> Banner.REPAIR_A11Y
        !micOn -> Banner.ALLOW_MIC
        snoozed -> Banner.END_SNOOZE
        else -> Banner.NONE
    }

    fun copy(banner: Banner): BannerCopy = when (banner) {
        Banner.REPAIR_A11Y -> BannerCopy(
            title = "Turn on Flow Bubble",
            body = "Open Accessibility, enable Open Flow, then return.",
            cta = "Open Accessibility",
            a11yLabel = "Turn on Flow Bubble. Open Accessibility.",
        )
        Banner.ALLOW_MIC -> BannerCopy(
            title = "Allow microphone",
            body = "Allow mic, then tap a text box and the bubble.",
            cta = "Allow microphone",
            a11yLabel = "Allow microphone.",
        )
        Banner.END_SNOOZE -> BannerCopy(
            title = "Bubble snoozed",
            body = null,
            cta = "End snooze",
            a11yLabel = "Bubble snoozed. End snooze.",
        )
        Banner.NONE -> BannerCopy(
            title = "",
            body = null,
            cta = null,
            a11yLabel = "",
        )
    }
}
