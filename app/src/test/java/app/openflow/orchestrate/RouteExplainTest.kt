package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RouteExplainTest {

    @Test
    fun formatsSystemLocalFirst() {
        val explain = RouteExplain(providerId = "system", reason = "local-first")
        assertThat(explain.toString()).isEqualTo("system: local-first")
    }
}
