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
    }

    @Test
    fun ui_kotlin_does_not_claim_transcript_never_leaves() {
        val src = UiSourceScan.uiKtText().lowercase()
        assertThat(src).doesNotContain("never uploads audio or transcripts")
        assertThat(src).doesNotContain("never uploaded by open flow")
        assertThat(src).doesNotContain("we do not upload")
        assertThat(src).doesNotContain("open flow never uploads audio")
        assertThat(src).contains("post this utterance")
    }
}
