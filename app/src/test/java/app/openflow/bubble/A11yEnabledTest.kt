package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class A11yEnabledTest {

    @Test
    fun flat_list_matches_component() {
        val cn = "app.openflow.debug/app.openflow.bubble.FlowAccessibilityService"
        assertThat(
            FlowAccessibilityService.enabledInSecureList(cn, cn)
        ).isTrue()
        assertThat(
            FlowAccessibilityService.enabledInSecureList(
                "com.other/.Svc:$cn",
                cn,
            )
        ).isTrue()
        assertThat(
            FlowAccessibilityService.enabledInSecureList("com.other/.Svc", cn)
        ).isFalse()
        assertThat(FlowAccessibilityService.enabledInSecureList(null, cn)).isFalse()
        assertThat(FlowAccessibilityService.enabledInSecureList("", cn)).isFalse()
    }
}
