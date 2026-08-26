package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ItnNumberTest {

    @Test
    fun simple_cardinals() {
        assertThat(ItnNumber.apply("twenty five people came")).isEqualTo("25 people came")
        assertThat(ItnNumber.apply("forty two")).isEqualTo("42")
        assertThat(ItnNumber.apply("he scored ninety nine")).isEqualTo("he scored 99")
        assertThat(ItnNumber.apply("we need at least twelve chairs"))
            .isEqualTo("we need at least 12 chairs")
    }

    @Test
    fun hundreds_and_thousands() {
        assertThat(ItnNumber.apply("one hundred and twenty three")).isEqualTo("123")
        assertThat(ItnNumber.apply("three hundred sixty five")).isEqualTo("365")
        assertThat(ItnNumber.apply("one thousand two hundred")).isEqualTo("1,200")
        assertThat(ItnNumber.apply("the number three hundred sixty five"))
            .isEqualTo("the number 365")
    }

    @Test
    fun decimals() {
        assertThat(ItnNumber.apply("three point one four")).isEqualTo("3.14")
        assertThat(ItnNumber.apply("two point five")).isEqualTo("2.5")
    }

    @Test
    fun years() {
        assertThat(ItnNumber.apply("in twenty twenty six")).isEqualTo("in 2026")
        assertThat(ItnNumber.apply("the year nineteen ninety nine"))
            .isEqualTo("the year 1999")
        assertThat(ItnNumber.apply("two thousand and twenty five")).isEqualTo("2025")
    }

    @Test
    fun scales_stay_words_when_standalone() {
        assertThat(ItnNumber.apply("seven billion people")).isEqualTo("7 billion people")
        assertThat(ItnNumber.apply("three hundred million users"))
            .isEqualTo("300 million users")
    }

    @Test
    fun ordinals_untouched() {
        assertThat(ItnNumber.apply("he finished first")).isEqualTo("he finished first")
        assertThat(ItnNumber.apply("the first quarter results"))
            .isEqualTo("the first quarter results")
    }

    @Test
    fun half_and_non_numbers_untouched() {
        assertThat(ItnNumber.apply("half of them agreed")).isEqualTo("half of them agreed")
        assertThat(ItnNumber.apply("no digits here at all")).isEqualTo("no digits here at all")
    }

    @Test
    fun fifty_fifty_idiom_not_a_year() {
        // 5050 outside plausible year window; decades don't stack → two separate 50s.
        assertThat(ItnNumber.apply("a fifty fifty split")).isEqualTo("a 50 50 split")
    }
}
