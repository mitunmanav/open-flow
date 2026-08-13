package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetentionProofTest {

    private val now = 1_800_000_000_000L
    private val dayMs = 24L * 60L * 60L * 1000L

    private val rows = listOf(
        RetentionProof.Row("old", now - dayMs - 1L),
        RetentionProof.Row("edge", now - dayMs),
        RetentionProof.Row("new", now - 1L),
    )

    @Test
    fun keep_keeps_all() {
        assertThat(RetentionProof.kept(rows, "keep", now))
            .containsExactly("old", "edge", "new")
            .inOrder()
    }

    @Test
    fun never_store_keeps_none() {
        assertThat(RetentionProof.kept(rows, "never_store", now)).isEmpty()
    }

    @Test
    fun wipe_24h_drops_older_than_24h_keeps_newer() {
        assertThat(RetentionProof.kept(rows, "wipe_24h", now))
            .containsExactly("edge", "new")
            .inOrder()
    }
}
