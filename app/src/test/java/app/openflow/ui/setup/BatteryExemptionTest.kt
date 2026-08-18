package app.openflow.ui.setup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BatteryExemptionTest {
    @Test
    fun request_uses_package_scoped_action() {
        assertThat(BatteryExemption.action(alreadyIgnoring = false))
            .isEqualTo(BatteryExemption.REQUEST)
        assertThat(BatteryExemption.action(alreadyIgnoring = true))
            .isEqualTo(BatteryExemption.APP_DETAILS)
        assertThat(BatteryExemption.action(false)).isNotEqualTo(BatteryExemption.ALL_APPS)
        assertThat(BatteryExemption.action(true)).isNotEqualTo(BatteryExemption.ALL_APPS)
        assertThat(BatteryExemption.fallbackAction()).isEqualTo(BatteryExemption.APP_DETAILS)
        assertThat(BatteryExemption.dataUri("app.openflow")).isEqualTo("package:app.openflow")
    }
}
