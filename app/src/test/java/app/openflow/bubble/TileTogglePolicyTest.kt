package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TileTogglePolicyTest {
    @Test
    fun dead_service_means_launch_app_not_toggle() {
        assertThat(TileTogglePolicy.nextHidden(serviceAlive = false, hidden = false)).isNull()
        assertThat(TileTogglePolicy.nextHidden(serviceAlive = false, hidden = true)).isNull()
    }

    @Test
    fun visible_bubble_hides() {
        assertThat(TileTogglePolicy.nextHidden(serviceAlive = true, hidden = false)).isTrue()
    }

    @Test
    fun hidden_bubble_unhides() {
        assertThat(TileTogglePolicy.nextHidden(serviceAlive = true, hidden = true)).isFalse()
    }
}
