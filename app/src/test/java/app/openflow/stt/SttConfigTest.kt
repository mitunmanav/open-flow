package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SttConfigTest {

    @Test
    fun defaults_prefer_offline() {
        val c = SttConfig()
        assertThat(c.preferOnDevice).isTrue()
        assertThat(c.extras()[SttConfig.KEY_PREFER_OFFLINE]).isEqualTo(true)
    }

    @Test
    fun can_disable_offline_preference() {
        val c = SttConfig(preferOnDevice = false)
        assertThat(c.extras()[SttConfig.KEY_PREFER_OFFLINE]).isEqualTo(false)
    }
}
