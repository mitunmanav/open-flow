package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeHowToCopyTest {
    @Test
    fun professional_simple_lines() {
        assertThat(HomeHowToCopy.title).isEqualTo("How Open Flow works")
        assertThat(HomeHowToCopy.lines).containsExactly(
            "Keep your keyboard. Tap the bubble to speak.",
            "Tap Done to insert. Tap Cancel to throw away.",
            "A dictionary entry is one word. A snippet is a whole block.",
        ).inOrder()
        assertThat(HomeHowToCopy.lines.joinToString(" ").lowercase())
            .doesNotContain("cmd")
        assertThat(HomeHowToCopy.lines.joinToString(" ")).doesNotContain("Dict =")
        assertThat(HomeHowToCopy.lines.joinToString(" ")).doesNotContain("X cancel")
    }
}
