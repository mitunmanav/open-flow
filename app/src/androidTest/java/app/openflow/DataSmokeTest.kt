package app.openflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.prefs.FlowPrefs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device smoke: prefs persist to SharedPreferences and dictations round-trip
 * through the real Room database + FTS index.
 */
@RunWith(AndroidJUnit4::class)
class DataSmokeTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun prefs_persist_across_instances() {
        val first = FlowPrefs(ctx)
        first.bubbleOpacity = 0.42f
        assertThat(FlowPrefs(ctx).bubbleOpacity).isEqualTo(0.42f)
        first.bubbleOpacity = 0.8f
    }

    @Test
    fun dictation_saves_and_searches_via_fts() = runBlocking {
        val repo = DictationRepository(
            OpenFlowDatabase.get(ctx),
            OpenFlowDatabase.get(ctx).dictationDao(),
            OpenFlowDatabase.get(ctx).dictationFtsDao(),
            OpenFlowDatabase.get(ctx).dictionaryDao(),
            OpenFlowDatabase.get(ctx).snippetDao(),
            OpenFlowDatabase.get(ctx).statsDao(),
            OpenFlowDatabase.get(ctx).voiceProfileDao(),
        )
        val marker = "zzsmoke${System.nanoTime()}"
        val saved = repo.saveDictation(
            rawText = "raw $marker",
            cleanText = "clean $marker text",
            durationMs = 1234L,
            languageTag = "en-US",
        )
        try {
            assertThat(saved).isNotNull()
            val hits = repo.searchDictations(marker)
            assertThat(hits.map { it.id }).contains(saved!!.id)
        } finally {
            saved?.let { repo.deleteDictation(it.id) }
        }
    }
}
