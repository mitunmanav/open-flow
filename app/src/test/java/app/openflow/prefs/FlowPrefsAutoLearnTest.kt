package app.openflow.prefs

import app.openflow.text.LearnEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class FlowPrefsAutoLearnTest {

    @Before
    fun resetLearn() {
        LearnEngine.resetLearn()
    }

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

    @Test
    fun learn_sides_roundtrip() {
        val store = MemoryPrefsStore()
        val prefs = FlowPrefs(store)
        assertThat(prefs.learnSides).isEmpty()
        val encoded = app.openflow.text.LearnEngine.encodeSides(
            sides = mapOf("mike" to setOf("turn")),
            auto = setOf("mike"),
            manual = emptySet()
        )
        prefs.learnSides = encoded
        assertThat(store.getString("learn_sides", "")).isEqualTo(encoded)
        val decoded = app.openflow.text.LearnEngine.decodeSides(prefs.learnSides)
        assertThat(decoded.sides["mike"]).containsExactly("turn")
        assertThat(decoded.auto).contains("mike")
        assertThat(decoded.manual).isEmpty()
    }
}
