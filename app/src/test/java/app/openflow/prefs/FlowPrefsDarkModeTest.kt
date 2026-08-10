package app.openflow.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure JVM tests — no Robolectric, no network jar download.
 */
class FlowPrefsDarkModeTest {

    @Test
    fun normalize_defaults_unknown_to_system() {
        assertThat(FlowPrefs.normalizeDarkMode("weird")).isEqualTo("system")
        assertThat(FlowPrefs.normalizeDarkMode("dark")).isEqualTo("dark")
        assertThat(FlowPrefs.normalizeDarkMode("light")).isEqualTo("light")
        assertThat(FlowPrefs.normalizeDarkMode("system")).isEqualTo("system")
    }

    @Test
    fun darkMode_defaults_to_system() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.darkMode.value).isEqualTo("system")
    }

    @Test
    fun setDarkMode_updates_flow_and_store() {
        val store = MemoryPrefsStore()
        val prefs = FlowPrefs(store)
        prefs.setDarkMode("dark")
        assertThat(prefs.darkMode.value).isEqualTo("dark")
        assertThat(store.getString("dark_mode", "")).isEqualTo("dark")
    }

    @Test
    fun setDarkMode_reads_back_from_store() {
        val store = MemoryPrefsStore()
        store.putString("dark_mode", "light")
        val prefs = FlowPrefs(store)
        assertThat(prefs.darkMode.value).isEqualTo("light")
    }

    @Test
    fun setDarkMode_rejects_garbage() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        prefs.setDarkMode("neon")
        assertThat(prefs.darkMode.value).isEqualTo("system")
    }
}
