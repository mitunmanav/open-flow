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
    fun moduleWhat_maps_home_ids() {
        assertThat(HomeFeelCopy.moduleWhat("setup")).isEqualTo("permissions")
        assertThat(HomeFeelCopy.moduleWhat("test")).isEqualTo("practice field")
        assertThat(HomeFeelCopy.moduleWhat("keys")).isEqualTo("cleanup chips")
        assertThat(HomeFeelCopy.moduleWhat("stats")).isEqualTo("last dictation")
        assertThat(HomeFeelCopy.moduleWhat("recent")).isEqualTo("history")
        assertThat(HomeFeelCopy.moduleWhat("history")).isEmpty()
    }
}
