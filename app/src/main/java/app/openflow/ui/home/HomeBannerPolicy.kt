package app.openflow.ui.home

object HomeBannerPolicy {
    enum class Banner { REPAIR_A11Y, ALLOW_MIC, END_SNOOZE, NONE }

    fun banner(bubbleOn: Boolean, micOn: Boolean, snoozed: Boolean): Banner = when {
        !bubbleOn -> Banner.REPAIR_A11Y
        !micOn -> Banner.ALLOW_MIC
        snoozed -> Banner.END_SNOOZE
        else -> Banner.NONE
    }
}
