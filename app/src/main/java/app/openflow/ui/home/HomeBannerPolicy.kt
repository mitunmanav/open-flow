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
            title = "Turn on the Flow Bubble",
            body = "Repair: Open Flow is not in Accessibility. Tap Enable bubble, turn it ON, then return here.",
            cta = "Open Accessibility",
            a11yLabel = "Turn on the Flow Bubble. Open Accessibility.",
        )
        Banner.ALLOW_MIC -> BannerCopy(
            title = "Allow the microphone",
            body = "Allow the microphone, then focus a field and tap the bubble.",
            cta = "Allow microphone",
            a11yLabel = "Allow the microphone.",
        )
        Banner.END_SNOOZE -> BannerCopy(
            title = "Bubble is snoozed",
            body = null,
            cta = "End snooze",
            a11yLabel = "Bubble is snoozed. End snooze.",
        )
        Banner.NONE -> BannerCopy(
            title = "",
            body = null,
            cta = null,
            a11yLabel = "",
        )
    }
}
