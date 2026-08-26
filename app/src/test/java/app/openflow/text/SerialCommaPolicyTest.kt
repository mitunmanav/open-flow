package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SerialCommaPolicyTest {

    @Test
    fun three_nouns_get_oxford_comma() {
        assertThat(SerialCommaPolicy.apply("we need milk eggs and bread"))
            .isEqualTo("we need milk, eggs, and bread")
    }

    @Test
    fun clause_and_is_untouched() {
        assertThat(SerialCommaPolicy.apply("I went home and slept"))
            .isEqualTo("I went home and slept")
    }

    @Test
    fun number_words_are_not_a_list() {
        assertThat(SerialCommaPolicy.apply("I have one apple and two bananas"))
            .isEqualTo("I have one apple and two bananas")
    }

    @Test
    fun already_comma_untouched() {
        val s = "milk, eggs and bread"
        assertThat(SerialCommaPolicy.apply(s)).isEqualTo(s)
    }
}
