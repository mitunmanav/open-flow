package app.openflow.engine

import app.openflow.orchestrate.AiWhen
import app.openflow.orchestrate.RouteMode
import app.openflow.prefs.MemoryPrefsStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnginePrefsAutoTest {

    @Test
    fun route_mode_defaults_local_then_ai() {
        val store = MemoryPrefsStore()
        val prefs = EnginePrefs(store)

        assertThat(prefs.routeMode).isEqualTo(RouteMode.LOCAL_THEN_AI)
        assertThat(prefs.brainId).isEqualTo("none")
        assertThat(prefs.autoRoute).isTrue()
        assertThat(prefs.aiWhen).isEqualTo(AiWhen.EVERY)

        prefs.routeMode = RouteMode.LOCAL_ONLY
        prefs.aiWhen = AiWhen.MISS_ONLY

        val again = EnginePrefs(store)
        assertThat(again.routeMode).isEqualTo(RouteMode.LOCAL_ONLY)
        assertThat(again.autoRoute).isFalse()
        assertThat(again.aiWhen).isEqualTo(AiWhen.MISS_ONLY)
    }
}
