package app.openflow.ui.setup

import app.openflow.ui.privacy.PrivacyHonesty

object FirstRunPolicy {
    enum class Step { A11Y, MIC, BATTERY, DONE }

    data class StepCopy(
        val title: String,
        val body: String,
        val primary: String,
        val secondary: String? = null,
    )

    fun step(bubbleOn: Boolean, micOn: Boolean, batterySeen: Boolean): Step = when {
        !bubbleOn -> Step.A11Y
        !micOn -> Step.MIC
        !batterySeen -> Step.BATTERY
        else -> Step.DONE
    }

    fun needsWizard(step: Step): Boolean = step != Step.DONE

    fun totalSteps(): Int = 3

    fun stepNumber(step: Step): Int = when (step) {
        Step.A11Y -> 1
        Step.MIC -> 2
        Step.BATTERY, Step.DONE -> 3
    }

    fun progressLabel(step: Step): String = when (step) {
        Step.DONE -> "Setup complete"
        else -> "Step ${stepNumber(step)} of ${totalSteps()}"
    }

    fun copy(step: Step): StepCopy = when (step) {
        Step.A11Y -> StepCopy(
            title = "Turn on the Flow Bubble",
            body = "Accessibility lets Open Flow insert text in any app. Keep your keyboard.",
            primary = "Open Accessibility",
        )
        Step.MIC -> StepCopy(
            title = "Allow the microphone",
            body = PrivacyHonesty.SETUP_MIC,
            primary = "Allow microphone",
        )
        Step.BATTERY -> StepCopy(
            title = "Keep the bubble alive",
            body = "Optional. Stop the phone from killing Open Flow. Tap Skip if you want to finish now.",
            primary = "Battery settings",
            secondary = "Skip",
        )
        Step.DONE -> StepCopy(
            title = "Setup complete",
            body = "Focus a text field and tap the bubble.",
            primary = "Done",
        )
    }

    fun a11yLabel(step: Step): String {
        val c = copy(step)
        return when (step) {
            Step.DONE -> c.title
            Step.BATTERY -> "${progressLabel(step)}. ${c.title}. Optional. You can Skip."
            else -> "${progressLabel(step)}. ${c.title}."
        }
    }
}
