package app.openflow.whisper

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class WhisperRuntimeHolderTest {
    @Test
    fun same_file_reuses_runtime() {
        var made = 0
        val holder = WhisperRuntimeHolder { made++; Fake() }
        val f = File("m.bin")
        val a = holder.get(f)
        val b = holder.get(f)
        assertThat(a).isSameInstanceAs(b)
        assertThat(made).isEqualTo(1)
    }

    @Test
    fun release_frees_and_next_get_makes_new() {
        var made = 0
        val holder = WhisperRuntimeHolder { made++; Fake() }
        val f = File("m.bin")
        val first = holder.get(f) as Fake
        holder.release()
        assertThat(first.released).isTrue()
        holder.get(f)
        assertThat(made).isEqualTo(2)
    }

    private class Fake : WhisperRuntime {
        var released = false
        override fun isLoaded() = !released
        override fun transcribe(samples: FloatArray) = ""
        override fun release() {
            released = true
        }
    }
}
