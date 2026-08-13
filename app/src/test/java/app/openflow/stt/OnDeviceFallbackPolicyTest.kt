package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Launch: missing on-device STT must fail-soft → default recognizer, never crash. */
class OnDeviceFallbackPolicyTest {

    private val p = OnDeviceFallbackPolicy()

    @Test
    fun prefer_on_device_when_available_and_not_fallen_back() {
        assertThat(
            p.tryOnDeviceFactory(
                preferOnDevice = true,
                forceOfflineOnly = true,
                offlineFallbackUsed = false,
                onDeviceAvailable = true,
            )
        ).isTrue()
    }

    @Test
    fun skip_on_device_when_unavailable() {
        assertThat(
            p.tryOnDeviceFactory(
                preferOnDevice = true,
                forceOfflineOnly = true,
                offlineFallbackUsed = false,
                onDeviceAvailable = false,
            )
        ).isFalse()
    }

    @Test
    fun skip_on_device_after_fallback() {
        assertThat(
            p.tryOnDeviceFactory(
                preferOnDevice = true,
                forceOfflineOnly = false,
                offlineFallbackUsed = true,
                onDeviceAvailable = true,
            )
        ).isFalse()
    }

    @Test
    fun factory_exception_is_non_fatal() {
        assertThat(p.factoryFailureIsFatal()).isFalse()
    }

    @Test
    fun language_errors_trigger_soft_fallback() {
        assertThat(p.shouldSoftFallback(ContinuousPolicy.ERROR_LANGUAGE_NOT_SUPPORTED)).isTrue()
        assertThat(p.shouldSoftFallback(ContinuousPolicy.ERROR_LANGUAGE_UNAVAILABLE)).isTrue()
        assertThat(p.shouldSoftFallback(ContinuousPolicy.ERROR_CLIENT)).isTrue()
        assertThat(p.shouldSoftFallback(ContinuousPolicy.ERROR_NETWORK)).isTrue()
    }

    @Test
    fun permissions_error_no_soft_fallback() {
        assertThat(p.shouldSoftFallback(ContinuousPolicy.ERROR_INSUFFICIENT_PERMISSIONS))
            .isFalse()
    }

    @Test
    fun soft_fallback_only_once() {
        assertThat(p.canSoftFallback(alreadyUsed = false)).isTrue()
        assertThat(p.canSoftFallback(alreadyUsed = true)).isFalse()
    }
}
