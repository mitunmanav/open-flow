package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryRowActionsTest {

    @Test
    fun primary_is_copy_share_only() {
        assertThat(HistoryRowActions.primary()).containsExactly("Copy", "Share").inOrder()
    }

    @Test
    fun more_without_raw() {
        assertThat(HistoryRowActions.more(hasRaw = false))
            .containsExactly("Edit", "Delete").inOrder()
    }

    @Test
    fun more_with_raw() {
        assertThat(HistoryRowActions.more(hasRaw = true))
            .containsExactly("Edit", "Show raw", "Use raw", "Delete").inOrder()
    }
}
