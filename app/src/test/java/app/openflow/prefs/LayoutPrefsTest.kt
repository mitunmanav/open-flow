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
        val m = LayoutPrefs.parseModules("setup,!stats,keys,test,recent", LayoutPrefs.HOME_MODULES)
        assertThat(m.find { it.id == "stats" }!!.visible).isFalse()
        assertThat(m.find { it.id == "setup" }!!.visible).isTrue()
    }

    @Test
    fun encode_roundtrip() {
        val raw = "setup,!stats,keys,test,recent"
        val m = LayoutPrefs.parseModules(raw, LayoutPrefs.HOME_MODULES)
        assertThat(LayoutPrefs.encodeModules(m)).isEqualTo(raw)
    }

    @Test
    fun move_up_down() {
        val m = LayoutPrefs.parseModules("setup,stats,keys,test,recent", LayoutPrefs.HOME_MODULES)
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
    fun drawer_settings_and_home_always() {
        assertThat(LayoutPrefs.isDrawerVisible("!history", "home")).isTrue()
        assertThat(LayoutPrefs.isDrawerVisible("!history", "settings")).isTrue()
        assertThat(LayoutPrefs.isDrawerVisible("!history,customize", "history")).isFalse()
    }

    @Test
    fun bottom_tabs_never_drawer() {
        assertThat(LayoutPrefs.isDrawerVisible("history,customize", "dictionary")).isFalse()
        assertThat(LayoutPrefs.isDrawerVisible("history,customize", "style")).isFalse()
        assertThat(LayoutPrefs.isDrawerVisible("history,customize", "snippets")).isFalse()
    }

    @Test
    fun old_nav_ids_dropped_from_drawer_catalog() {
        val m = LayoutPrefs.parseModules(
            "history,dictionary,snippets,style,settings",
            LayoutPrefs.DRAWER_EXTRAS
        )
        assertThat(m.map { it.id }).containsExactly("history", "customize").inOrder()
    }
}
