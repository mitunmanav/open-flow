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

    @Test
    fun progress_labels_for_launch() {
        assertThat(WalkthroughPolicy.totalPages()).isEqualTo(5)
        assertThat(WalkthroughPolicy.pageNumber(WalkthroughPolicy.Page.WHAT)).isEqualTo(1)
        assertThat(WalkthroughPolicy.pageNumber(WalkthroughPolicy.Page.READY)).isEqualTo(5)
        assertThat(WalkthroughPolicy.progressLabel(WalkthroughPolicy.Page.TALK))
            .isEqualTo("Page 2 of 5")
        assertThat(WalkthroughPolicy.nextLabel(WalkthroughPolicy.Page.READY)).isEqualTo("Done")
        assertThat(WalkthroughPolicy.nextLabel(WalkthroughPolicy.Page.WHAT)).isEqualTo("Next")
    }

    @Test
    fun copy_not_ime_and_privacy_clear() {
        val what = WalkthroughPolicy.copy(WalkthroughPolicy.Page.WHAT)
        assertThat(what.title).isEqualTo("Speak to type")
        assertThat(what.body).contains("Not a new keyboard")
        val privacy = WalkthroughPolicy.copy(WalkthroughPolicy.Page.PRIVACY)
        assertThat(privacy.body.lowercase()).contains("post")
        assertThat(privacy.body.lowercase()).doesNotContain("do not upload")
        val ready = WalkthroughPolicy.copy(WalkthroughPolicy.Page.READY)
        assertThat(ready.body).contains("bubble")
        assertThat(WalkthroughPolicy.a11yLabel(WalkthroughPolicy.Page.WHAT))
            .isEqualTo("Page 1 of 5. Speak to type.")
    }
}
