package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleShrinkPolicyTest {
    @Test
    fun master_off_is_full() {
        assertThat(BubbleShrinkPolicy.idleMode(masterOn = false, toDot = true)).isEqualTo("full")
        assertThat(
            BubbleShrinkPolicy.searchMul(
                masterOn = false,
                shrinkSearch = true,
                searchFocused = true,
                listening = false,
            )
        ).isEqualTo(1f)
    }

    @Test
    fun master_dot() {
        assertThat(BubbleShrinkPolicy.idleMode(masterOn = true, toDot = true)).isEqualTo("dot")
        assertThat(BubbleShrinkPolicy.idleMode(masterOn = true, toDot = false)).isEqualTo("compact")
    }

    @Test
    fun search_shrink_only_when_master() {
        assertThat(
            BubbleShrinkPolicy.searchMul(true, shrinkSearch = true, searchFocused = true, listening = false)
        ).isEqualTo(0.72f)
        assertThat(
            BubbleShrinkPolicy.searchMul(true, shrinkSearch = true, searchFocused = true, listening = true)
        ).isEqualTo(1f)
    }
}
