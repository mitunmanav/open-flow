package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiScrollPolicyTest {

    @Test
    fun history_row_key_is_id() {
        assertThat(UiScrollPolicy.historyRowKey("abc")).isEqualTo("abc")
    }

    @Test
    fun day_header_key_unique_when_label_repeats() {
        val a = UiScrollPolicy.dayHeaderKey("Earlier", "id-1")
        val b = UiScrollPolicy.dayHeaderKey("Earlier", "id-2")
        assertThat(a).isNotEqualTo(b)
        assertThat(a).contains("Earlier")
    }

    @Test
    fun dict_and_snippet_keys_prefixed() {
        assertThat(UiScrollPolicy.dictRowKey("w1")).isEqualTo("dict-w1")
        assertThat(UiScrollPolicy.snippetRowKey("s1")).isEqualTo("snip-s1")
    }
}
