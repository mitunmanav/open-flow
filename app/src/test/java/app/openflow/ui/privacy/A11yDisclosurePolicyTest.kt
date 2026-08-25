package app.openflow.ui.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class A11yDisclosurePolicyTest {

    @Test
    fun gate_shows_until_accepted() {
        assertThat(A11yDisclosurePolicy.shouldShow(accepted = false)).isTrue()
        assertThat(A11yDisclosurePolicy.shouldShow(accepted = true)).isFalse()
    }

    @Test
    fun copy_names_data_purpose_and_stays_local() {
        val c = A11yDisclosurePolicy.copy()
        assertThat(c.title.lowercase()).contains("accessibility")
        val body = c.body.lowercase()
        assertThat(body).contains("text field")
        assertThat(body).contains("package")
        assertThat(body).contains("audio")
        assertThat(body).contains("this phone")
        assertThat(body).contains("keyboard")
        assertThat(c.agree).contains("Agree")
        assertThat(c.decline).isNotEmpty()
    }
}
