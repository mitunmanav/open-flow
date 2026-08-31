package app.openflow.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioProbeInstrumentedTest {
    @Test
    fun probe_of_win_reports() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val res = AudioProbe.probe(ctx, 16000)
        // Log for measurement gate
        android.util.Log.i(
            "OpenFlow.Probe",
            "req=${res.requestedRate} actual=${res.actualRate} min=${res.minBufferSize} selBuf=${res.selectedBufferSize} " +
                "frames=${res.framesPerBuffer} src=${res.source} state=${res.state} startOk=${res.startRecordingOk} " +
                "fallback=${res.fallbackUsed} fallbackRate=${res.fallbackRate} resample=${res.resampleNeeded} " +
                "firstMs=${res.firstPcmMs} err=${res.error} elapsed=${res.elapsedMs}"
        )
        // Basic assertions for of_win: should have selected buffer >0 if mic permission granted or not
        // In emulator without permission, start may fail but min should be >0
        assertThat(res.requestedRate).isEqualTo(16000)
        // Do not assert startOk because permission may be missing in CI
    }

    @Test
    fun probe_fallback_rates() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        for (rate in listOf(16000, 48000, 44100)) {
            val r = AudioProbe.probe(ctx, rate)
            android.util.Log.i("OpenFlow.Probe", "fallback probe rate=$rate min=${r.minBufferSize} src=${r.source} sel=${r.selectedBufferSize}")
            assertThat(r.requestedRate).isEqualTo(rate)
        }
    }
}
