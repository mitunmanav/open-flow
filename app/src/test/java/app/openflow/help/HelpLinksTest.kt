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
    }
}
