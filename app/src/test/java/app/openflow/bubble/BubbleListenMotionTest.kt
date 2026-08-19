package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleListenMotionTest {

    @Test
    fun overlay_scale_stays_base_even_when_loud() {
        assertThat(BubbleListenMotion.overlayScale(base = 1f, rms = 10f)).isEqualTo(1f)
        assertThat(BubbleListenMotion.overlayScale(base = 0.85f, rms = 10f)).isEqualTo(0.85f)
    }
}
