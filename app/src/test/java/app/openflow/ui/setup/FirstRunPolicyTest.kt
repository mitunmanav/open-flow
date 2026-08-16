package app.openflow.ui.setup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FirstRunPolicyTest {

    @Test
    fun a11y_first() {
        assertThat(
            FirstRunPolicy.step(bubbleOn = false, micOn = false, batterySeen = false)
        ).isEqualTo(FirstRunPolicy.Step.A11Y)
    }

    @Test
    fun mic_after_a11y() {
        assertThat(
            FirstRunPolicy.step(bubbleOn = true, micOn = false, batterySeen = false)
        ).isEqualTo(FirstRunPolicy.Step.MIC)
    }

    @Test
    fun battery_after_mic_once() {
        assertThat(
            FirstRunPolicy.step(bubbleOn = true, micOn = true, batterySeen = false)
        ).isEqualTo(FirstRunPolicy.Step.BATTERY)
    }

    @Test
    fun done_when_ready_and_battery_seen() {
        assertThat(
            FirstRunPolicy.step(bubbleOn = true, micOn = true, batterySeen = true)
        ).isEqualTo(FirstRunPolicy.Step.DONE)
    }

    @Test
    fun wizard_needed_until_done() {
        assertThat(FirstRunPolicy.needsWizard(FirstRunPolicy.Step.A11Y)).isTrue()
        assertThat(FirstRunPolicy.needsWizard(FirstRunPolicy.Step.DONE)).isFalse()
    }

    @Test
    fun launch_flow_is_a11y_then_mic_then_battery() {
        assertThat(FirstRunPolicy.totalSteps()).isEqualTo(3)
        assertThat(FirstRunPolicy.stepNumber(FirstRunPolicy.Step.A11Y)).isEqualTo(1)
        assertThat(FirstRunPolicy.stepNumber(FirstRunPolicy.Step.MIC)).isEqualTo(2)
        assertThat(FirstRunPolicy.stepNumber(FirstRunPolicy.Step.BATTERY)).isEqualTo(3)
        assertThat(FirstRunPolicy.progressLabel(FirstRunPolicy.Step.A11Y))
            .isEqualTo("Step 1 of 3")
        assertThat(FirstRunPolicy.progressLabel(FirstRunPolicy.Step.MIC))
            .isEqualTo("Step 2 of 3")
        assertThat(FirstRunPolicy.progressLabel(FirstRunPolicy.Step.BATTERY))
            .isEqualTo("Step 3 of 3")
        assertThat(FirstRunPolicy.progressLabel(FirstRunPolicy.Step.DONE))
            .isEqualTo("Setup complete")
    }

    @Test
    fun battery_step_copy_makes_skip_clear() {
        val c = FirstRunPolicy.copy(FirstRunPolicy.Step.BATTERY)
        assertThat(c.title).isEqualTo("Stop the phone killing it")
        assertThat(c.body).contains("Optional")
        assertThat(c.body).contains("Skip")
        assertThat(c.primary).isEqualTo("Battery settings")
        assertThat(c.secondary).isEqualTo("Skip")
    }

    @Test
    fun a11y_and_mic_copy_keep_keyboard() {
        val a11y = FirstRunPolicy.copy(FirstRunPolicy.Step.A11Y)
        assertThat(a11y.body).contains("keyboard")
        assertThat(a11y.primary).isEqualTo("Open Accessibility")
        val mic = FirstRunPolicy.copy(FirstRunPolicy.Step.MIC)
        assertThat(mic.body.lowercase()).contains("post")
        assertThat(mic.body.lowercase()).doesNotContain("never uploads")
        assertThat(mic.primary).isEqualTo("Allow microphone")
        assertThat(mic.secondary).isNull()
    }

    @Test
    fun a11y_label_includes_step_and_title() {
        assertThat(FirstRunPolicy.a11yLabel(FirstRunPolicy.Step.A11Y))
            .isEqualTo("Step 1 of 3. Turn on Flow Bubble.")
        assertThat(FirstRunPolicy.a11yLabel(FirstRunPolicy.Step.BATTERY))
            .contains("Optional")
        assertThat(FirstRunPolicy.a11yLabel(FirstRunPolicy.Step.BATTERY))
            .contains("Skip")
    }

    @Test
    fun setupMic_primary_isAllowMicrophone() {
        assertThat(FirstRunPolicy.copy(FirstRunPolicy.Step.MIC).primary)
            .isEqualTo("Allow microphone")
    }

    @Test
    fun primary_verbs_never_ok_or_continue_for_a11y_mic() {
        val forbidden = setOf("OK", "Continue")
        listOf(FirstRunPolicy.Step.A11Y, FirstRunPolicy.Step.MIC).forEach { step ->
            assertThat(FirstRunPolicy.copy(step).primary).isNotIn(forbidden)
        }
    }
}
