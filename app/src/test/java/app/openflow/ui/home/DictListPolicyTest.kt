package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DictListPolicyTest {

    @Test
    fun newest_first() {
        val rows = listOf(1L to "b", 3L to "a", 2L to "c")
        assertThat(DictListPolicy.sort(rows, DictListPolicy.Sort.NEWEST).map { it.second })
            .containsExactly("a", "c", "b").inOrder()
    }

    @Test
    fun oldest_first() {
        val rows = listOf(1L to "b", 3L to "a")
        assertThat(DictListPolicy.sort(rows, DictListPolicy.Sort.OLDEST).map { it.second })
            .containsExactly("b", "a").inOrder()
    }

    @Test
    fun alphabetical() {
        val rows = listOf(1L to "zeta", 2L to "Alpha", 3L to "beta")
        assertThat(DictListPolicy.sort(rows, DictListPolicy.Sort.ALPHA).map { it.second })
            .containsExactly("Alpha", "beta", "zeta").inOrder()
    }
}
