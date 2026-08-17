package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnDeviceSpeechPolicyTest {

    @Test
    fun default_pref_is_off() {
        assertThat(OnDeviceSpeechPolicy.DEFAULT_PREFER).isFalse()
    }

    @Test
    fun flags_off_never_force_offline() {
        val flags = OnDeviceSpeechPolicy.flags(
            preferOnDevice = false,
            offlineFallbackUsed = false,
        )
        assertThat(flags.preferOnDevice).isFalse()
        assertThat(flags.forceOfflineOnly).isFalse()
    }

    @Test
    fun flags_on_force_offline_until_fallback() {
        val before = OnDeviceSpeechPolicy.flags(
            preferOnDevice = true,
            offlineFallbackUsed = false,
        )
        assertThat(before.preferOnDevice).isTrue()
        assertThat(before.forceOfflineOnly).isTrue()
        val after = OnDeviceSpeechPolicy.flags(
            preferOnDevice = true,
            offlineFallbackUsed = true,
        )
        assertThat(after.forceOfflineOnly).isFalse()
    }

    @Test
    fun honesty_off_admits_audio_may_leave() {
        val line = OnDeviceSpeechPolicy.honesty(preferOnDevice = false)
        assertThat(line).isEqualTo(OnDeviceSpeechPolicy.HONESTY_OFF)
        assertThat(line.lowercase()).contains("may")
        assertThat(line.lowercase()).doesNotContain("never")
    }

    @Test
    fun honesty_on_says_os_pack_fail_soft() {
        val line = OnDeviceSpeechPolicy.honesty(preferOnDevice = true)
        assertThat(line).isEqualTo(OnDeviceSpeechPolicy.HONESTY_ON)
        assertThat(line.lowercase()).contains("on-device")
        assertThat(line.lowercase()).contains("fail-soft")
    }
}
