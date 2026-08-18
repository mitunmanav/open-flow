package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleScaleStepsTest {
    @Test
    fun nearest_snaps_to_steps() {
        assertThat(BubbleScaleSteps.nearest(0.72f)).isEqualTo(0.70f)
        assertThat(BubbleScaleSteps.nearest(0.90f)).isEqualTo(0.85f)
        assertThat(BubbleScaleSteps.nearest(1.00f)).isEqualTo(1.00f)
        assertThat(BubbleScaleSteps.nearest(1.20f)).isEqualTo(1.15f)
        assertThat(BubbleScaleSteps.DEFAULT).isEqualTo(0.85f)
    }
}
