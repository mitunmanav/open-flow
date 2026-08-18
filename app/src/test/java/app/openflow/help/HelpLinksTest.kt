package app.openflow.help

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HelpLinksTest {
    @Test
    fun urls_point_at_open_flow_repo() {
        assertThat(HelpLinks.DISCUSSIONS).contains("mitunmanav/open-flow/discussions")
        assertThat(HelpLinks.ISSUES_NEW).contains("issues/new/choose")
        assertThat(HelpLinks.SECURITY_ADVISORY).contains("security/advisories")
        assertThat(HelpLinks.SECURITY_ADVISORY).doesNotContain("issues/new")
        assertThat(HelpLinks.SITE).isEqualTo("https://mitunmanav.github.io/open-flow/")
    }

    @Test
    fun no_personal_contact() {
        val blob = listOf(
            HelpLinks.DISCUSSIONS,
            HelpLinks.ISSUES_NEW,
            HelpLinks.SECURITY_ADVISORY,
            HelpLinks.SITE,
        ).joinToString("\n").lowercase()
        assertThat(blob).doesNotContain("gmail")
        assertThat(blob).doesNotContain("mailto:")
        assertThat(blob).doesNotContain("@")
    }
}
