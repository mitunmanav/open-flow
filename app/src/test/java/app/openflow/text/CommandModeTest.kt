package app.openflow.text

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CommandModeTest {

    @Test
    fun bullet_list_command_formats_items() = runTest {
        val input = "make bullets buy groceries comma send email comma call mom"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("• Buy groceries\n• Send email\n• Call mom")
    }

    @Test
    fun numbered_list_command_formats_items() = runTest {
        val input = "numbered list first step comma second step comma third step"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("1. First step\n2. Second step\n3. Third step")
    }

    @Test
    fun all_caps_command_converts_text() = runTest {
        val input = "all caps urgent message"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("URGENT MESSAGE")
    }

    @Test
    fun lowercase_command_converts_text() = runTest {
        val input = "all lower THIS WAS ALL CAPS"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("this was all caps")
    }

    @Test
    fun title_case_command_converts_text() = runTest {
        val input = "title case the quick brown fox jumps over the lazy dog"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("The Quick Brown Fox Jumps Over The Lazy Dog")
    }

    @Test
    fun quote_command_wraps_in_quotes() = runTest {
        val input = "add quotes to be or not to be"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("\"to be or not to be\"")
    }

    @Test
    fun camel_case_command_formats_code_identifier() = runTest {
        val input = "camel case get user profile data"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("getUserProfileData")
    }

    @Test
    fun snake_case_command_formats_code_identifier() = runTest {
        val input = "snake case get user profile data"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("get_user_profile_data")
    }

    @Test
    fun non_command_text_is_untouched() = runTest {
        val input = "just a regular sentence about things"
        val out = CommandMode.applyLocal(input)
        assertThat(out).isEqualTo("just a regular sentence about things")
    }

    @Test
    fun organize_into_bullets_formats_items() = runTest {
        val out = CommandMode.applyLocal("organize into bullets milk comma eggs")
        assertThat(out).isEqualTo("• Milk\n• Eggs")
    }

    @Test
    fun make_a_list_formats_items() = runTest {
        val out = CommandMode.applyLocal("make a list milk comma eggs")
        assertThat(out).isEqualTo("• Milk\n• Eggs")
    }
}
