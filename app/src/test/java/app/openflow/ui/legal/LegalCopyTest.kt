package app.openflow.ui.legal

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LegalCopyTest {

    @Test
    fun privacy_is_honest_and_local_first() {
        val p = LegalCopy.privacyBody.lowercase()
        assertThat(p).contains("no account")
        assertThat(p).contains("no ads")
        assertThat(p).contains("accessibility")
        assertThat(p).contains("microphone")
        assertThat(p).contains("internet")
        assertThat(p).contains("post")
        assertThat(p).doesNotContain("never upload")
        assertThat(p).doesNotContain("no internet permission")
    }

    @Test
    fun terms_cover_mit_and_use() {
        val t = LegalCopy.termsBody.lowercase()
        assertThat(t).contains("mit")
        assertThat(t).contains("as is")
        assertThat(t).contains("accessibility")
        assertThat(t).contains("keyboard")
        assertThat(t).contains("13")
        assertThat(LegalCopy.privacyTitle).isEqualTo("Privacy policy")
        assertThat(LegalCopy.termsTitle).isEqualTo("Terms of use")
    }

    @Test
    fun contact_is_github_not_personal() {
        val blob = (LegalCopy.privacyBody + "\n" + LegalCopy.termsBody).lowercase()
        assertThat(blob).contains("discussions")
        assertThat(blob).contains("issues")
        assertThat(blob).doesNotContain("issues only")
        assertThat(blob).doesNotContain("gmail")
        assertThat(blob).doesNotContain("mailto")
        assertThat(blob).doesNotContain("@")
    }
}
