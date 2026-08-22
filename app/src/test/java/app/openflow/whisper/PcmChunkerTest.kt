package app.openflow.whisper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PcmChunkerTest {
    @Test
    fun emits_full_chunk_keeps_remainder() {
        val c = PcmChunker(chunkSamples = 4)
        assertThat(c.push(floatArrayOf(1f, 2f, 3f))).isEmpty()
        val out = c.push(floatArrayOf(4f, 5f))
        assertThat(out.single().toList()).containsExactly(1f, 2f, 3f, 4f)
        assertThat(c.flush().toList()).containsExactly(5f)
    }

    @Test
    fun flush_empty_when_nothing_buffered() {
        assertThat(PcmChunker(8).flush()).isEmpty()
    }

    @Test
    fun never_drops_samples() {
        val c = PcmChunker(chunkSamples = 3)
        val a = c.push(FloatArray(10) { it.toFloat() })
        val tail = c.flush()
        val all = a.flatMap { it.toList() } + tail.toList()
        assertThat(all).containsExactlyElementsIn((0..9).map { it.toFloat() }).inOrder()
    }
}
