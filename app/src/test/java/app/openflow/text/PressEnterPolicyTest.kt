package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PressEnterPolicyTest {

    @Test
    fun mid_sentence_stays() {
        val r = PressEnterPolicy.apply("press enter the door")
        assertThat(r.submit).isFalse()
        assertThat(r.text).isEqualTo("press enter the door")
    }

    @Test
    fun trailing_press_enter_strips_and_submits() {
        val r = PressEnterPolicy.apply("Hello world. Press enter")
        assertThat(r.submit).isTrue()
        assertThat(r.text).isEqualTo("Hello world.")
    }

    @Test
    fun trailing_with_punct() {
        val r = PressEnterPolicy.apply("send it press enter.")
        assertThat(r.submit).isTrue()
        assertThat(r.text).isEqualTo("send it")
    }

    @Test
    fun blank_no_submit() {
        val r = PressEnterPolicy.apply("   ")
        assertThat(r.submit).isFalse()
        assertThat(r.text).isEqualTo("   ")
    }
}
