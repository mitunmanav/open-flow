package app.openflow.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlowPrefsAutoLearnTest {

    @Test
    fun autoLearn_defaults_true() {
        val prefs = FlowPrefs(MemoryPrefsStore())
        assertThat(prefs.autoLearn).isTrue()
    }

    @Test
    fun autoLearn_persists_false() {
        val store = MemoryPrefsStore()
        val prefs = FlowPrefs(store)
        prefs.autoLearn = false
        assertThat(prefs.autoLearn).isFalse()
        assertThat(store.getString("auto_learn", "true")).isEqualTo("false")
    }
}
