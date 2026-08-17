package app.openflow.ui.privacy

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyHonestyTest {

    @Test
    fun copy_admits_brain_can_post() {
        assertThat(PrivacyHonesty.HOME_FOOTER.lowercase()).contains("post")
        assertThat(PrivacyHonesty.SETTINGS_BODY.lowercase()).contains("post")
        assertThat(PrivacyHonesty.KEEP_FOREVER.lowercase()).contains("post")
        assertThat(PrivacyHonesty.HOME_FOOTER.lowercase()).doesNotContain("never upload")
        assertThat(PrivacyHonesty.SETTINGS_BODY.lowercase()).doesNotContain("never upload")
        assertThat(PrivacyHonesty.KEEP_FOREVER.lowercase()).doesNotContain("never uploaded")
        // Cloud ear is opt-in — do not claim it is off for this version.
        assertThat(PrivacyHonesty.SETTINGS_BODY.lowercase()).doesNotContain("off in 0.1")
        assertThat(PrivacyHonesty.SETTINGS_BODY.lowercase()).contains("until you pick")
    }

    @Test
    fun on_device_toggle_copy_is_honest() {
        assertThat(PrivacyHonesty.ON_DEVICE_OFF.lowercase()).contains("may")
        assertThat(PrivacyHonesty.ON_DEVICE_OFF.lowercase()).contains("google")
        assertThat(PrivacyHonesty.ON_DEVICE_ON.lowercase()).contains("on-device")
        assertThat(PrivacyHonesty.ON_DEVICE_ON.lowercase()).doesNotContain("never")
    }

    @Test
    fun insights_voice_admits_counts_not_never() {
        assertThat(PrivacyHonesty.INSIGHTS_VOICE.lowercase()).contains("counts")
        assertThat(PrivacyHonesty.INSIGHTS_VOICE.lowercase()).doesNotContain("never")
    }
}
