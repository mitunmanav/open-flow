package app.openflow

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import app.openflow.ui.MainActivity
import org.junit.Rule
import org.junit.Test

/** Smoke: the app launches into one of its real first-run states without crashing. */
class AppLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launches_into_walkthrough_setup_or_home() {
        composeRule.waitUntil(15_000) {
            val tags = listOf("walkthrough", "setup_wizard", "home_hub", "nav_bar")
            runCatching {
                tags.any { tag ->
                    composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
                }
            }.getOrDefault(false)
        }
    }
}
