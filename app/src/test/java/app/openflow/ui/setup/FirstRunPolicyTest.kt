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
}
