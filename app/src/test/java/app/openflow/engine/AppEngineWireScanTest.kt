package app.openflow.engine

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class AppEngineWireScanTest {
    @Test
    fun wire_does_not_register_stub_on_device_ear() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/OpenFlowApp.kt",
        ).readText()
        val wire = src.substringAfter("object AppEngineWire")
        assertThat(wire).doesNotContain("OnDeviceEar()")
        assertThat(wire).doesNotContain("registerEar(EarId.ON_PHONE)")
    }
}
