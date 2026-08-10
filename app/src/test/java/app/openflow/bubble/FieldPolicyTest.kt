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
    fun merge_insert_adds_space() {
        assertThat(FieldPolicy.mergeInsert("Hello", "world")).isEqualTo("Hello world")
    }

    @Test
    fun merge_empty_base() {
        assertThat(FieldPolicy.mergeInsert("", "hi")).isEqualTo("hi")
    }

    @Test
    fun edittext_class_is_editable() {
        assertThat(FieldPolicy.isEditableClass("android.widget.EditText")).isTrue()
    }
}
