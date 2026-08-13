package app.openflow.privacy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetentionPolicyTest {

    @Test
    fun policy_names_are_exact() {
        assertThat(RetentionPolicy.KEEP).isEqualTo("keep")
        assertThat(RetentionPolicy.WIPE_24H).isEqualTo("wipe_24h")
        assertThat(RetentionPolicy.NEVER_STORE).isEqualTo("never_store")
    }

    @Test
    fun wipe_window_is_exactly_24h_ms() {
        assertThat(RetentionPolicy.WIPE_WINDOW_MS).isEqualTo(86_400_000L)
        assertThat(RetentionPolicy.WIPE_WINDOW_MS).isNotEqualTo(86_400_001L)
        assertThat(RetentionPolicy.WIPE_WINDOW_MS).isNotEqualTo(86_399_999L)
    }

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
            .isEqualTo(now - 86_400_000L)
    }

    @Test
    fun cutoff_off_by_one_is_wrong() {
        val now = 1_800_000_000_000L
        val cut = RetentionPolicy.cutoffEpochMs(now, "wipe_24h")
        assertThat(cut).isEqualTo(now - RetentionPolicy.WIPE_WINDOW_MS)
        assertThat(cut).isNotEqualTo(now - 86_400_001L)
        assertThat(cut).isNotEqualTo(now - 86_399_999L)
    }

    @Test
    fun never_stores_audio() {
        assertThat(RetentionPolicy.storesAudio("keep")).isFalse()
        assertThat(RetentionPolicy.storesAudio("wipe_24h")).isFalse()
        assertThat(RetentionPolicy.storesAudio("never_store")).isFalse()
    }
}
