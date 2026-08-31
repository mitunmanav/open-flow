package app.openflow.prefs

import app.openflow.ui.HomeFeelCopy
import app.openflow.ui.home.HomeModulePolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LayoutPrefsTest {

    @Test
    fun default_parse_matches_catalog_order_and_declutter_visibility() {
        val m = LayoutPrefs.parseModules(LayoutPrefs.DEFAULT_HOME, LayoutPrefs.HOME_MODULES)
        assertThat(m.map { it.id }).isEqualTo(LayoutPrefs.HOME_MODULES)
        assertThat(m.find { it.id == "banner" }!!.visible).isTrue()
        assertThat(m.find { it.id == "search" }!!.visible).isTrue()
        assertThat(m.find { it.id == "recent" }!!.visible).isTrue()
        assertThat(m.find { it.id == "howto" }!!.visible).isTrue()
        assertThat(m.find { it.id == "stats" }!!.visible).isFalse()
        assertThat(m.find { it.id == "note" }!!.visible).isFalse()
        assertThat(m.find { it.id == "honesty" }!!.visible).isTrue()
    }

    @Test
    fun hidden_prefix() {
        val m = LayoutPrefs.parseModules("banner,!search,recent", LayoutPrefs.HOME_MODULES)
        assertThat(m.find { it.id == "search" }!!.visible).isFalse()
        assertThat(m.find { it.id == "banner" }!!.visible).isTrue()
    }

    @Test
    fun encode_roundtrip() {
        val raw = "banner,search,recent,howto,!stats,!note,honesty"
        val m = LayoutPrefs.parseModules(raw, LayoutPrefs.HOME_MODULES)
        assertThat(LayoutPrefs.encodeModules(m)).isEqualTo(raw)
    }

    @Test
    fun move_up_down() {
        val m = LayoutPrefs.parseModules("banner,search,recent", LayoutPrefs.HOME_MODULES)
        val down = LayoutPrefs.move(m, "banner", 1)
        assertThat(down.map { it.id }.take(2)).isEqualTo(listOf("search", "banner"))
        val up = LayoutPrefs.move(down, "banner", -1)
        assertThat(up.map { it.id }.first()).isEqualTo("banner")
    }

    @Test
    fun toggle() {
        val m = LayoutPrefs.parseModules(LayoutPrefs.DEFAULT_HOME, LayoutPrefs.HOME_MODULES)
        val t = LayoutPrefs.toggleVisible(m, "search")
        assertThat(t.find { it.id == "search" }!!.visible).isFalse()
    }

    @Test
    fun migrate_legacy_setup_keys_test() {
        val m = LayoutPrefs.parseModules("setup,test,keys,stats,recent", LayoutPrefs.HOME_MODULES)
        assertThat(m.map { it.id }).containsAtLeast("howto", "search", "stats", "recent").inOrder()
        assertThat(m.none { it.id == "setup" || it.id == "keys" || it.id == "test" }).isTrue()
        assertThat(m.find { it.id == "howto" }!!.visible).isTrue()
        assertThat(m.find { it.id == "search" }!!.visible).isTrue()
        // Missing banner/note/honesty fill from declutter defaults
        assertThat(m.find { it.id == "note" }!!.visible).isFalse()
        assertThat(m.find { it.id == "stats" }!!.visible).isTrue()
    }

    @Test
    fun normalizeHomeRaw_empty_is_default() {
        assertThat(LayoutPrefs.normalizeHomeRaw("")).isEqualTo(LayoutPrefs.DEFAULT_HOME)
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

    @Test
    fun homeModulePolicy_ordered_visible_skips_hidden() {
        val m = LayoutPrefs.parseModules(LayoutPrefs.DEFAULT_HOME, LayoutPrefs.HOME_MODULES)
        assertThat(HomeModulePolicy.orderedVisible(m))
            .containsExactly("banner", "search", "recent", "howto", "honesty")
            .inOrder()
        assertThat(HomeModulePolicy.isVisible(m, HomeModulePolicy.STATS)).isFalse()
        assertThat(HomeModulePolicy.isVisible(m, HomeModulePolicy.NOTE)).isFalse()
    }

    @Test
    fun moduleWhat_maps_current_home_ids() {
        assertThat(HomeFeelCopy.moduleWhat("banner")).isEqualTo("status banner")
        assertThat(HomeFeelCopy.moduleWhat("search")).isEqualTo("search transcripts")
        assertThat(HomeFeelCopy.moduleWhat("recent")).isEqualTo("history")
        assertThat(HomeFeelCopy.moduleWhat("howto")).isEqualTo("first-run tip")
        assertThat(HomeFeelCopy.moduleWhat("stats")).isEqualTo("word stats")
        assertThat(HomeFeelCopy.moduleWhat("note")).isEqualTo("local note")
        assertThat(HomeFeelCopy.moduleWhat("honesty")).isEqualTo("privacy footer")
        assertThat(HomeFeelCopy.moduleWhat("setup")).isEmpty()
    }
}
