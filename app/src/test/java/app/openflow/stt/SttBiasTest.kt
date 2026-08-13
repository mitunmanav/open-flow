package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SttBiasTest {

    @Test
    fun fieldTokens_drops_blank_common_short() {
        val tokens = SttBias.fieldTokens("  hi the a x world  ")
        assertThat(tokens).containsExactly("hi", "world").inOrder()
    }

    @Test
    fun strings_include_dict_keys_values_and_field() {
        val out = SttBias.strings(
            dictionary = mapOf("Mitton" to "Mitun", "the" to "x"),
            fieldTokens = SttBias.fieldTokens("hello john")
        )
        assertThat(out).containsAtLeast("Mitton", "Mitun", "hello", "john")
        assertThat(out).doesNotContain("the")
        assertThat(out).doesNotContain("x")
    }

    @Test
    fun strings_distinct_and_cap_max() {
        val dict = (1..50).associate { i ->
            "key${i.toString().padStart(2, '0')}" to "val${i.toString().padStart(2, '0')}"
        }
        val field = (1..40).map { "fld${it.toString().padStart(2, '0')}" }
        val out = SttBias.strings(dict, field)
        assertThat(out).hasSize(SttBias.MAX)
        assertThat(out.distinct()).hasSize(out.size)
    }

    @Test
    fun strings_prefer_longer_when_truncating() {
        val longWords = (1..40).associate { i ->
            "longword${i.toString().padStart(2, '0')}xxxx" to
                "longrepl${i.toString().padStart(2, '0')}yyyy"
        }
        val shortField = (1..50).map { "s$it" }
        val out = SttBias.strings(longWords, shortField)
        assertThat(out).hasSize(SttBias.MAX)
        assertThat(out.all { it.length >= 8 }).isTrue()
        assertThat(out.none { it.startsWith("s") && it.length <= 3 }).isTrue()
    }
}
