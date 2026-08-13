package app.openflow.ui.setup

object FirstRunPolicy {
    enum class Step { A11Y, MIC, BATTERY, DONE }

    fun step(bubbleOn: Boolean, micOn: Boolean, batterySeen: Boolean): Step = when {
        !bubbleOn -> Step.A11Y
        !micOn -> Step.MIC
        !batterySeen -> Step.BATTERY
        else -> Step.DONE
    }

    fun needsWizard(step: Step): Boolean = step != Step.DONE
}
