package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CommandChromeTest {

    @Test
    fun visible_only_when_command_enabled() {
        assertThat(CommandChrome.visible(commandEnabled = true)).isTrue()
        assertThat(CommandChrome.visible(commandEnabled = false)).isFalse()
    }

    @Test
    fun suffix_always_empty() {
        assertThat(CommandChrome.suffix(commandEnabled = false)).isEmpty()
        assertThat(CommandChrome.suffix(commandEnabled = true)).isEmpty()
    }
}
