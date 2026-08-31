package app.openflow.prefs

import app.openflow.ui.HomeFeelCopy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure JVM tests — no Robolectric, no network jar download.
 */
class FlowPrefsSeenHowToTest {

    @Test
    fun seenHowTo_defaults_false() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.seenHowTo).isFalse()
    }

    @Test
    fun seenHowTo_persists_true() {
        val store = MemoryPrefsStore()
        val prefs = FlowPrefs(store)
        prefs.seenHowTo = true
        assertThat(prefs.seenHowTo).isTrue()
        assertThat(store.getString("seen_how_to", "false")).isEqualTo("true")
    }

    @Test
    fun homeModules_migrates_legacy_and_persists() {
        val store = MemoryPrefsStore()
        store.putString("home_layout", "setup,test,keys,stats,recent")
        val prefs = FlowPrefs(store)
        val m = prefs.homeModules()
        assertThat(m.none { it.id == "setup" || it.id == "keys" || it.id == "test" }).isTrue()
        assertThat(m.any { it.id == "howto" && it.visible }).isTrue()
        assertThat(m.any { it.id == "search" && it.visible }).isTrue()
        assertThat(store.getString("home_layout", "")).doesNotContain("setup")
    }

    @Test
    fun moduleWhat_maps_home_ids() {
        assertThat(HomeFeelCopy.moduleWhat("banner")).isEqualTo("status banner")
        assertThat(HomeFeelCopy.moduleWhat("search")).isEqualTo("search transcripts")
        assertThat(HomeFeelCopy.moduleWhat("recent")).isEqualTo("history")
        assertThat(HomeFeelCopy.moduleWhat("howto")).isEqualTo("first-run tip")
        assertThat(HomeFeelCopy.moduleWhat("stats")).isEqualTo("word stats")
        assertThat(HomeFeelCopy.moduleWhat("note")).isEqualTo("local note")
        assertThat(HomeFeelCopy.moduleWhat("honesty")).isEqualTo("privacy footer")
        assertThat(HomeFeelCopy.moduleWhat("history")).isEmpty()
    }
}
