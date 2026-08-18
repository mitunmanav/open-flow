package app.openflow.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProcessStatusTest {
    @Test
    fun normalize_and_failed() {
        assertThat(ProcessStatus.normalize(null)).isEqualTo(ProcessStatus.OK)
        assertThat(ProcessStatus.normalize("failed")).isEqualTo(ProcessStatus.FAILED)
        assertThat(ProcessStatus.isFailed("failed")).isTrue()
        assertThat(ProcessStatus.isFailed("ok")).isFalse()
    }
}
