package app.openflow.text

import app.openflow.stt.HypothesisPick
import app.openflow.stt.SttBias
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * T14 integration: a pair confirmed after N consistent hits reaches the P2
 * vocabulary stage (dictionary rewrite) and the P1 N-best pick bias.
 */
class LearnedPairsFeedTest {

    @Before
    fun resetLearn() {
        LearnEngine.resetLearn()
    }

    @Test
    fun confirmed_pair_feeds_vocabulary_and_pick_bias() {
        val from = "Mitton"
        val to = "Mitun"
        assertThat(LearnEngine.noteAutoCandidate(from, to, setOf("meet")).persisted).isFalse()
        val confirm = LearnEngine.noteAutoCandidate(from, to, setOf("meet"))
        assertThat(confirm.persisted).isTrue()

        // P2 vocabulary stage: dictionary rewrite honors the learned pair.
        val dict = mapOf(from to to)
        val rewritten = TextPostProcessor.applyDictionary(
            "meet Mitton tomorrow",
            dict,
            sides = LearnEngine.sideBags(),
            autoKeys = LearnEngine.autoKeys(),
        )
        assertThat(rewritten).isEqualTo("meet Mitun tomorrow")

        // P1 pick bias: learned terms reach the re-rank dictionary...
        val bias = SttBias.strings(dict, emptyList())
        assertThat(bias).contains("Mitton")
        assertThat(bias).contains("Mitun")

        // ...and flip the N-best choice toward the learned spelling over non-terms.
        val picked = HypothesisPick.best(
            listOf("say Smith please", "say Mitun please"),
            scores = null,
            dictionary = bias.toSet(),
        )
        assertThat(picked).isEqualTo("say Mitun please")
    }

    @Test
    fun single_hit_stays_pending_never_confirms() {
        val first = LearnEngine.noteAutoCandidate("wisper", "Wispr", setOf("try"))
        assertThat(first.persisted).isFalse()
        assertThat(LearnEngine.autoKeys()).doesNotContain("wisper")
        // N-hit confirmation kept: one hit is not enough; second identical hit confirms.
        val second = LearnEngine.noteAutoCandidate("wisper", "Wispr", setOf("try"))
        assertThat(second.persisted).isTrue()
        assertThat(LearnEngine.autoKeys()).contains("wisper")
    }
}
