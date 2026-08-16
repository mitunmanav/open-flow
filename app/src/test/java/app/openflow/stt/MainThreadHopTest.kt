package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/** Pure helper matching FlowAccessibilityService cloud-ear UI hop. */
object MainThreadHop {
    fun run(isMain: Boolean, post: (() -> Unit) -> Unit, block: () -> Unit) {
        if (isMain) block() else post(block)
    }
}

class MainThreadHopTest {
    @Test
    fun runs_inline_when_already_main() {
        var ran = false
        MainThreadHop.run(isMain = true, post = { error("should not post") }) { ran = true }
        assertThat(ran).isTrue()
    }

    @Test
    fun posts_when_off_main() {
        val posted = AtomicBoolean(false)
        var ran = false
        MainThreadHop.run(isMain = false, post = { block ->
            posted.set(true)
            block()
        }) { ran = true }
        assertThat(posted.get()).isTrue()
        assertThat(ran).isTrue()
    }
}
