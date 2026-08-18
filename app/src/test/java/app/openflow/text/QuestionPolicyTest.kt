package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuestionPolicyTest {

    @Test
    fun mid_text_second_sentence_asks() {
        val out = QuestionPolicy.applyAll("Go now. Can we meet")
        assertThat(out).contains("Can we meet?")
        assertThat(out).contains("Go now")
    }

    @Test
    fun how_we_built_long_not_question() {
        val s = "How we built this together today."
        assertThat(QuestionPolicy.apply(s)).isEqualTo(s)
    }

    @Test
    fun how_are_you_is_question() {
        assertThat(QuestionPolicy.apply("How are you")).endsWith("?")
    }

    @Test
    fun tag_right() {
        assertThat(QuestionPolicy.apply("You're coming right")).endsWith("?")
    }
}
