package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeStatsPagerTest {
    @Test
    fun three_pages_words_sessions_streak() {
        val pages = HomeStatsPager.pages(10, 2, 3)
        assertThat(pages.map { it.label }).isEqualTo(listOf("Words", "Sessions", "Streak"))
        assertThat(pages[0].value).isEqualTo(HomeStatsCopy.words(10))
        assertThat(pages[1].value).isEqualTo(HomeStatsCopy.sessions(2))
        assertThat(pages[2].value).isEqualTo(HomeStatsCopy.streak(3))
    }

    @Test
    fun index_clamps() {
        assertThat(HomeStatsPager.clamp(0, 3)).isEqualTo(0)
        assertThat(HomeStatsPager.clamp(9, 3)).isEqualTo(2)
        assertThat(HomeStatsPager.clamp(-1, 3)).isEqualTo(0)
        assertThat(HomeStatsPager.clamp(0, 0)).isEqualTo(0)
    }
}
