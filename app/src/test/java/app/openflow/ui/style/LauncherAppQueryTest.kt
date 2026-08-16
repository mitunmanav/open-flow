package app.openflow.ui.style

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LauncherAppQueryTest {
    @Test
    fun filter_matches_label_or_package() {
        val apps = listOf(
            LauncherApp("com.slack", "Slack", null),
            LauncherApp("com.whatsapp", "WhatsApp", null),
        )
        assertThat(LauncherAppQuery.filter(apps, "slack").map { it.packageName })
            .containsExactly("com.slack")
        assertThat(LauncherAppQuery.filter(apps, "COM.WHAT").map { it.packageName })
            .containsExactly("com.whatsapp")
        assertThat(LauncherAppQuery.filter(apps, "")).hasSize(2)
    }
}
