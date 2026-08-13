package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FieldPolicyTest {

    @Test
    fun password_is_sensitive() {
        assertThat(FieldPolicy.isSensitive(true, 1, "EditText", null)).isTrue()
    }

    @Test
    fun normal_edit_not_sensitive() {
        assertThat(
            FieldPolicy.isSensitive(false, 1, "android.widget.EditText", "Message")
        ).isFalse()
    }

    @Test
    fun phone_input_type_sensitive() {
        assertThat(FieldPolicy.isSensitive(false, 0x3, "EditText", null)).isTrue()
    }

    @Test
    fun number_input_type_sensitive() {
        assertThat(FieldPolicy.isSensitive(false, 0x2, "EditText", null)).isTrue()
    }

    @Test
    fun password_input_variation_sensitive_even_if_flag_false() {
        // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD = 0x81
        assertThat(FieldPolicy.isSensitive(false, 0x81, "EditText", null)).isTrue()
    }

    @Test
    fun visible_password_variation_sensitive() {
        // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x91
        assertThat(FieldPolicy.isSensitive(false, 0x91, "EditText", null)).isTrue()
    }

    @Test
    fun web_password_variation_sensitive() {
        // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_WEB_PASSWORD = 0xe1
        assertThat(FieldPolicy.isSensitive(false, 0xe1, "EditText", null)).isTrue()
    }

    @Test
    fun phone_hint_without_edit_in_class_is_sensitive() {
        assertThat(
            FieldPolicy.isSensitive(false, 0, "android.view.View", "Phone number")
        ).isTrue()
    }

    @Test
    fun skip_hints_ignore_body_text() {
        assertThat(FieldPolicy.skipHints(hintText = "Message", contentDescription = null))
            .isEqualTo("Message")
        assertThat(FieldPolicy.skipHints(hintText = null, contentDescription = "To"))
            .isEqualTo("To")
        assertThat(
            FieldPolicy.isSensitive(
                false,
                1,
                "android.widget.EditText",
                FieldPolicy.skipHints("Message", null)
            )
        ).isFalse()
    }

    @Test
    fun merge_insert_adds_space() {
        assertThat(FieldPolicy.mergeInsert("Hello", "world")).isEqualTo("Hello world")
    }

    @Test
    fun merge_empty_base() {
        assertThat(FieldPolicy.mergeInsert("", "hi")).isEqualTo("hi")
    }

    @Test
    fun merge_session_once() {
        assertThat(FieldPolicy.mergeSession("Hello", "world")).isEqualTo("Hello world")
        assertThat(FieldPolicy.mergeSession("", "only")).isEqualTo("only")
        assertThat(FieldPolicy.mergeSession("Hi", "")).isEqualTo("Hi")
    }

    @Test
    fun merge_session_no_double_space_before_punct() {
        assertThat(FieldPolicy.mergeSession("Hi", ".")).isEqualTo("Hi.")
    }

    @Test
    fun edittext_class_is_editable() {
        assertThat(FieldPolicy.isEditableClass("android.widget.EditText")).isTrue()
    }

    @Test
    fun merge_session_skips_overlapping_prefix() {
        val piece = "Does naren know if ram know if narendra on the call?"
        assertThat(FieldPolicy.mergeSession("Does", piece)).isEqualTo(piece)
    }

    @Test
    fun merge_session_prefix_overlap_ignore_case() {
        assertThat(FieldPolicy.mergeSession("does", "Does naren know"))
            .isEqualTo("Does naren know")
    }

    @Test
    fun search_hint_is_search() {
        assertThat(FieldPolicy.isSearch(0x1, "android.widget.EditText", "Search chats")).isTrue()
    }

    @Test
    fun message_field_not_search() {
        assertThat(FieldPolicy.isSearch(0x1, "android.widget.EditText", "Message")).isFalse()
    }

    @Test
    fun web_edit_text_is_not_search() {
        // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_WEB_EDIT_TEXT = 0xa1
        assertThat(FieldPolicy.isSearch(0xa1, "android.widget.EditText", "Message")).isFalse()
    }

    @Test
    fun filter_variation_is_search() {
        // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_FILTER = 0xb1
        assertThat(FieldPolicy.isSearch(0xb1, "android.widget.EditText", null)).isTrue()
    }
}
