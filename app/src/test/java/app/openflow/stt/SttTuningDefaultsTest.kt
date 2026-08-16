package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SttTuningDefaultsTest {

    @Test
    fun default_language_is_en_us() {
        assertThat(SttTuning.DEFAULT_LANGUAGE).isEqualTo("en-US")
    }

    @Test
    fun balanced_is_faster_than_old_quality_defaults() {
        val t = SttTuning()
        assertThat(t.completeSilenceMs).isAtMost(1000L)
        assertThat(t.possiblyCompleteSilenceMs).isAtMost(600L)
        assertThat(t.preferFormattingQuality).isTrue()
    }

    @Test
    fun fast_shorter_than_balanced() {
        val f = SttTuning.forProfile(SttTuning.PROFILE_FAST)
        val b = SttTuning.forProfile(SttTuning.PROFILE_BALANCED)
        assertThat(f.completeSilenceMs).isLessThan(b.completeSilenceMs)
        assertThat(f.minSpeechMs).isAtMost(b.minSpeechMs)
    }

    @Test
    fun accurate_longer_and_quality() {
        val a = SttTuning.forProfile(SttTuning.PROFILE_ACCURATE)
        assertThat(a.preferFormattingQuality).isTrue()
        assertThat(a.completeSilenceMs).isGreaterThan(SttTuning().completeSilenceMs)
    }

    @Test
    fun normalize_profile() {
        assertThat(SttTuning.normalizeProfile("FAST")).isEqualTo("fast")
        assertThat(SttTuning.normalizeProfile("nope")).isEqualTo("balanced")
    }
}
