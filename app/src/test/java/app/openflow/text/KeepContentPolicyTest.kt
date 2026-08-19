package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeepContentPolicyTest {

    @Test
    fun keep_non_blank() {
        assertThat(KeepContentPolicy.decide("send it", explicitWipe = false, hasContentWords = true))
            .isEqualTo(KeepContentPolicy.Kind.KEEP_CLEAN)
    }

    @Test
    fun wipe_may_empty() {
        assertThat(KeepContentPolicy.decide("", explicitWipe = true, hasContentWords = true))
            .isEqualTo(KeepContentPolicy.Kind.ALLOW_EMPTY)
    }

    @Test
    fun fillers_only_may_empty() {
        assertThat(KeepContentPolicy.decide("", explicitWipe = false, hasContentWords = false))
            .isEqualTo(KeepContentPolicy.Kind.ALLOW_EMPTY)
    }

    @Test
    fun real_words_recover() {
        assertThat(KeepContentPolicy.decide("", explicitWipe = false, hasContentWords = true))
            .isEqualTo(KeepContentPolicy.Kind.RECOVER)
    }
}
