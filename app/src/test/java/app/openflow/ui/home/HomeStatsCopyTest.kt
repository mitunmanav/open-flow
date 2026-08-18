package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeStatsCopyTest {
    @Test
    fun streak_sentence() {
        assertThat(HomeStatsCopy.streak(3)).isEqualTo("3-day streak")
        assertThat(HomeStatsCopy.words(10L)).isEqualTo("10 words")
        assertThat(HomeStatsCopy.sessions(2L)).isEqualTo("2 sessions")
    }
}
