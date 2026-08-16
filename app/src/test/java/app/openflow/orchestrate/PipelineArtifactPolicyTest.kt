package app.openflow.orchestrate

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PipelineArtifactPolicyTest {
    @Test
    fun brainThrows_bestAvailableUsesCleaned() = runTest {
        val artifact = PipelineArtifactPolicy.build(
            raw = "um hello",
            cleaned = "Hello.",
        ) {
            error("brain failed")
        }

        assertThat(artifact.ai).isEmpty()
        assertThat(artifact.bestAvailable()).isEqualTo("Hello.")
    }
}
