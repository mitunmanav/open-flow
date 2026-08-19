package app.openflow.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.OpenCard

@Composable
fun HomeStats(
    words: Long,
    sessions: Long,
    streak: Int,
) {
    val pages = HomeStatsPager.pages(words, sessions, streak)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scheme = MaterialTheme.colorScheme

    OpenCard(modifier = Modifier.testTag("home_stats")) {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(HomeFeedTokens.cardInnerGap),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_stats_pager"),
            ) { i ->
                val page = pages[HomeStatsPager.clamp(i, pages.size)]
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        page.value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        softWrap = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_stats_value"),
                    )
                    Text(
                        page.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        softWrap = true,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEachIndexed { i, _ ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(8.dp)
                            .background(
                                if (pagerState.currentPage == i) scheme.onSurface else scheme.outline,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}
