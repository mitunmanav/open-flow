package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InsertSubmitPolicyTest {

    @Test
    fun no_submit_is_none() {
        assertThat(InsertSubmitPolicy.how(submit = false, api = 36))
            .isEqualTo(InsertSubmitPolicy.How.NONE)
    }

    @Test
    fun api30_uses_ime_enter() {
        assertThat(InsertSubmitPolicy.how(submit = true, api = 30))
            .isEqualTo(InsertSubmitPolicy.How.IME_ENTER)
        assertThat(InsertSubmitPolicy.how(submit = true, api = 29))
            .isEqualTo(InsertSubmitPolicy.How.CLICK)
        assertThat(InsertSubmitPolicy.actionId(InsertSubmitPolicy.How.IME_ENTER))
            .isEqualTo(0x00400000)
        assertThat(InsertSubmitPolicy.actionId(InsertSubmitPolicy.How.CLICK))
            .isEqualTo(0x00000010)
        assertThat(InsertSubmitPolicy.actionId(InsertSubmitPolicy.How.NONE)).isNull()
    }
}
