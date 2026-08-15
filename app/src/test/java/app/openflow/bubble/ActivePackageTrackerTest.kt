package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ActivePackageTrackerTest {

    @Test
    fun remember_event_package_clears_stale_bank() {
        val next = ActivePackageTracker.remember(
            last = "com.phonepe.app",
            eventPackage = "app.openflow.debug",
        )
        assertThat(next).isEqualTo("app.openflow.debug")
        assertThat(PackagePolicy.shouldHideBubble(next)).isFalse()
    }

    @Test
    fun remember_keeps_last_when_event_blank() {
        assertThat(ActivePackageTracker.remember("com.phonepe.app", null))
            .isEqualTo("com.phonepe.app")
        assertThat(ActivePackageTracker.remember("com.phonepe.app", "  "))
            .isEqualTo("com.phonepe.app")
    }

    @Test
    fun bankGate_blocks_focus_only_while_bank_active() {
        assertThat(ActivePackageTracker.shouldIgnoreFocus("com.phonepe.app")).isTrue()
        assertThat(ActivePackageTracker.shouldIgnoreFocus("app.openflow.debug")).isFalse()
    }
}
