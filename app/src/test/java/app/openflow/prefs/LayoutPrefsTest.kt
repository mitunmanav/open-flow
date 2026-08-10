package app.openflow.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LayoutPrefsTest {

    @Test
    fun default_parse_all_visible_in_order() {
        val m = LayoutPrefs.parseModules(LayoutPrefs.DEFAULT_HOME, LayoutPrefs.HOME_MODULES)
        assertThat(m.map { it.id }).isEqualTo(LayoutPrefs.HOME_MODULES)
        assertThat(m.all { it.visible }).isTrue()
    }

    @Test
    fun hidden_prefix() {
        val m = LayoutPrefs.parseModules("setup,!stats,test,recent", LayoutPrefs.HOME_MODULES)
        assertThat(m.find { it.id == "stats" }!!.visible).isFalse()
        assertThat(m.find { it.id == "setup" }!!.visible).isTrue()
    }

    @Test
    fun encode_roundtrip() {
        val raw = "setup,!stats,test,recent"
        val m = LayoutPrefs.parseModules(raw, LayoutPrefs.HOME_MODULES)
        assertThat(LayoutPrefs.encodeModules(m)).isEqualTo(raw)
    }

    @Test
    fun move_up_down() {
        val m = LayoutPrefs.parseModules("setup,stats,test,recent", LayoutPrefs.HOME_MODULES)
        val down = LayoutPrefs.move(m, "setup", 1)
        assertThat(down.map { it.id }.take(2)).isEqualTo(listOf("stats", "setup"))
        val up = LayoutPrefs.move(down, "setup", -1)
        assertThat(up.map { it.id }.first()).isEqualTo("setup")
    }

    @Test
    fun toggle() {
        val m = LayoutPrefs.parseModules(LayoutPrefs.DEFAULT_HOME, LayoutPrefs.HOME_MODULES)
        val t = LayoutPrefs.toggleVisible(m, "test")
        assertThat(t.find { it.id == "test" }!!.visible).isFalse()
    }

    @Test
    fun nav_home_always() {
        assertThat(LayoutPrefs.isNavVisible("!history", "home")).isTrue()
        assertThat(LayoutPrefs.isNavVisible("!history,dictionary,snippets,style,settings", "history")).isFalse()
    }
}
