package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceCommandsJsonSyncTest {

    @Test
    fun three_json_copies_byte_equal() {
        fun read(p: String) = java.io.File(p).readText(Charsets.UTF_8)
        val assets = read("src/main/assets/voice_commands.json")
        assertThat(read("src/main/resources/voice_commands.json")).isEqualTo(assets)
        assertThat(read("src/test/resources/voice_commands.json")).isEqualTo(assets)
    }
}
