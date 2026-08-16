package app.openflow.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeNotePrefsTest {
    @Test
    fun homeNote_defaults_empty() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.homeNote).isEmpty()
    }

    @Test
    fun homeNote_persists() {
        val store = MemoryPrefsStore()
        val prefs = FlowPrefs(store)
        prefs.homeNote = "scratch idea"
        assertThat(prefs.homeNote).isEqualTo("scratch idea")
        assertThat(FlowPrefs(store).homeNote).isEqualTo("scratch idea")
    }
}
