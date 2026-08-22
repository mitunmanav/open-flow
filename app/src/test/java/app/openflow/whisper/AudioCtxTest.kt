package app.openflow.whisper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioCtxTest {
    @Test
    fun empty_uses_min() {
        assertThat(AudioCtx.frames(0)).isEqualTo(150)
        assertThat(AudioCtx.frames(-1)).isEqualTo(150)
    }

    @Test
    fun one_second_uses_min() {
        assertThat(AudioCtx.frames(16_000)).isEqualTo(150)
    }

    @Test
    fun three_seconds_is_raw_plus_pad() {
        // 3s * 50 fps = 150, +16 pad = 166
        assertThat(AudioCtx.frames(48_000)).isEqualTo(166)
    }

    @Test
    fun ten_seconds() {
        // 10s * 50 = 500 + 16 = 516
        assertThat(AudioCtx.frames(160_000)).isEqualTo(516)
    }

    @Test
    fun thirty_seconds_caps() {
        assertThat(AudioCtx.frames(480_000)).isEqualTo(1500)
    }

    @Test
    fun longer_than_thirty_caps() {
        assertThat(AudioCtx.frames(1_000_000)).isEqualTo(1500)
    }
}
