package app.openflow.engine

import app.openflow.prefs.MemoryPrefsStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnginePrefsAutoTest {

    @Test
    fun auto_route_defaults_false_and_persists_true() {
        val store = MemoryPrefsStore()
        val prefs = EnginePrefs(store)

        assertThat(prefs.autoRoute).isFalse()

        prefs.autoRoute = true

        assertThat(EnginePrefs(store).autoRoute).isTrue()
    }
}
