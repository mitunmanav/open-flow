package app.openflow.ui.home

import app.openflow.ui.a11y.Dimen
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeFeedTokensTest {
    @Test
    fun tokens_match_dimen_scale() {
        assertThat(HomeFeedTokens.sectionGap).isEqualTo(Dimen.GAP)
        assertThat(HomeFeedTokens.cardInnerGap).isEqualTo(Dimen.GAP)
        assertThat(HomeFeedTokens.chipGap).isEqualTo(Dimen.GAP_SM)
        assertThat(HomeFeedTokens.pagePadH).isEqualTo(Dimen.PAGE_PAD)
        assertThat(HomeFeedTokens.pagePadV).isEqualTo(Dimen.GAP)
    }
}
