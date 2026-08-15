package app.openflow.ui.home

import app.openflow.data.FtsQuery
import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class HistorySearchPolicyTest {

    @Test
    fun blank_means_recent_list_not_fts() {
        assertThat(HistorySearchPolicy.ftsMatch("")).isNull()
        assertThat(HistorySearchPolicy.ftsMatch("   ")).isNull()
        assertThat(HistorySearchPolicy.ftsMatch("***")).isNull()
    }

    @Test
    fun typed_query_is_fts_sanitize_for_repo() {
        assertThat(HistorySearchPolicy.ftsMatch("tiger"))
            .isEqualTo(FtsQuery.sanitize("tiger"))
        assertThat(HistorySearchPolicy.ftsMatch("open flow"))
            .isEqualTo(FtsQuery.sanitize("open flow"))
        assertThat(HistorySearchPolicy.ftsMatch("safe* AND \"token\""))
            .isEqualTo(FtsQuery.sanitize("safe* AND \"token\""))
    }

    @Test
    fun history_screen_calls_repo_fts_not_in_memory_contains() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/ui/MainActivity.kt"
        ).readText()
        assertThat(src).contains("searchDictations")
        assertThat(src).contains("HistorySearchPolicy.ftsMatch")
        assertThat(src).doesNotContain("contains(searchQuery")
        assertThat(src).contains("HistoryExport")
    }
}
