package app.openflow.ui.walkthrough

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WalkthroughPolicyTest {

    @Test
    fun five_pages_in_order() {
        assertThat(WalkthroughPolicy.pages()).containsExactly(
            WalkthroughPolicy.Page.WHAT,
            WalkthroughPolicy.Page.TALK,
            WalkthroughPolicy.Page.DICT_VS_SNIP,
            WalkthroughPolicy.Page.PRIVACY,
            WalkthroughPolicy.Page.READY,
        ).inOrder()
    }

    @Test
    fun seen_true_skips_walkthrough() {
        assertThat(WalkthroughPolicy.needsWalkthrough(seen = true)).isFalse()
    }

    @Test
    fun unseen_needs_walkthrough() {
        assertThat(WalkthroughPolicy.needsWalkthrough(seen = false)).isTrue()
    }
}
