package app.openflow.bubble

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class ReceiverExportPolicyTest {
    private fun serviceSrc() =
        File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt",
        ).readText()

    @Test
    fun copy_flags_are_not_exported() {
        assertThat(ReceiverExportPolicy.copyFlags())
            .isEqualTo(ReceiverExportPolicy.NOT_EXPORTED)
    }

    @Test
    fun inject_flags_are_not_exported() {
        assertThat(ReceiverExportPolicy.injectFlags())
            .isEqualTo(ReceiverExportPolicy.NOT_EXPORTED)
    }

    @Test
    fun inject_allowed_only_when_debug() {
        assertThat(ReceiverExportPolicy.injectAllowed(debug = false)).isFalse()
        assertThat(ReceiverExportPolicy.injectAllowed(debug = true)).isTrue()
    }

    @Test
    fun service_does_not_register_copy_exported() {
        val src = serviceSrc()
        assertThat(src).doesNotContain(
            "registerReceiver(copyReceiver, filter, Context.RECEIVER_EXPORTED)",
        )
        assertThat(src).contains("ReceiverExportPolicy.copyFlags()")
    }

    @Test
    fun service_does_not_register_inject_exported() {
        val src = serviceSrc()
        assertThat(src).doesNotContain(
            "registerReceiver(injectReceiver, filter, Context.RECEIVER_EXPORTED)",
        )
        assertThat(src).contains("ReceiverExportPolicy.injectFlags()")
    }
}
