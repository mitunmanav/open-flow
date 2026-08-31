package app.openflow.ui.home

import app.openflow.prefs.LayoutPrefs
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeModulePolicyTest {

    @Test
    fun defaults_hide_stats_and_note() {
        val m = LayoutPrefs.parseModules(LayoutPrefs.DEFAULT_HOME, LayoutPrefs.HOME_MODULES)
        assertThat(HomeModulePolicy.isVisible(m, HomeModulePolicy.STATS)).isFalse()
        assertThat(HomeModulePolicy.isVisible(m, HomeModulePolicy.NOTE)).isFalse()
        assertThat(HomeModulePolicy.isVisible(m, HomeModulePolicy.SEARCH)).isTrue()
        assertThat(HomeModulePolicy.isVisible(m, HomeModulePolicy.RECENT)).isTrue()
    }
}
