package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StutterCollapseTest {

    @Test
    fun letter_stutter_drops_before_word() {
        assertThat(StutterCollapse.apply("w w why")).isEqualTo("why")
    }

    @Test
    fun emphatic_triple_no_is_kept() {
        assertThat(StutterCollapse.apply("no no no i insist"))
            .isEqualTo("no, no, no, i insist")
    }

    @Test
    fun emphatic_quad_no_is_kept() {
        assertThat(StutterCollapse.apply("no no no no i insist"))
            .isEqualTo("no no no no, i insist")
    }

    @Test
    fun function_double_collapses() {
        assertThat(StutterCollapse.apply("the the meeting")).isEqualTo("the meeting")
    }

    @Test
    fun content_double_emphasis_kept() {
        assertThat(StutterCollapse.apply("very very good")).isEqualTo("very very good")
    }
}
