package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetentionPolicyTest {

    @Test
    fun keep_stores_and_has_no_cutoff() {
        assertThat(RetentionPolicy.shouldPersist("keep")).isTrue()
        assertThat(RetentionPolicy.cutoffEpochMs(1_000_000L, "keep")).isNull()
    }

    @Test
    fun never_store_skips() {
        assertThat(RetentionPolicy.shouldPersist("never_store")).isFalse()
        assertThat(RetentionPolicy.cutoffEpochMs(1_000_000L, "never_store")).isNull()
    }

    @Test
    fun wipe_24h_stores_and_cuts_yesterday() {
        val now = 1_800_000_000_000L
        assertThat(RetentionPolicy.shouldPersist("wipe_24h")).isTrue()
        assertThat(RetentionPolicy.cutoffEpochMs(now, "wipe_24h"))
            .isEqualTo(now - 24L * 60L * 60L * 1000L)
    }
}
