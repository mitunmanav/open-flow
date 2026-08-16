package app.openflow.bubble

import app.openflow.text.WritingStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppContextEngineTest {

    @Test
    fun messaging_packages_classified_correctly() {
        val messaging = listOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.instagram.android",
            "com.facebook.orca",
            "com.discord",
            "com.google.android.apps.messaging",
        )
        for (pkg in messaging) {
            val ctx = AppContextEngine.detect(pkg, null)
            assertThat(ctx.category).isEqualTo(AppCategory.MESSAGING)
            assertThat(ctx.defaultStyle).isEqualTo(WritingStyle.CASUAL)
            assertThat(ctx.promptHint).contains("messaging")
        }
    }

    @Test
    fun email_packages_classified_correctly() {
        val email = listOf(
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.fsck.k9",
            "ch.protonmail.android",
            "com.yahoo.mobile.client.android.mail",
        )
        for (pkg in email) {
            val ctx = AppContextEngine.detect(pkg, null)
            assertThat(ctx.category).isEqualTo(AppCategory.EMAIL)
            assertThat(ctx.defaultStyle).isEqualTo(WritingStyle.FORMAL)
            assertThat(ctx.promptHint).contains("email")
        }
    }

    @Test
    fun work_and_collab_packages_classified_correctly() {
        val work = listOf(
            "com.Slack",
            "com.microsoft.teams",
            "com.linkedin.android",
            "com.atlassian.jira",
            "com.asana.app",
        )
        for (pkg in work) {
            val ctx = AppContextEngine.detect(pkg, null)
            assertThat(ctx.category).isEqualTo(AppCategory.WORK_COLLAB)
            assertThat(ctx.defaultStyle).isEqualTo(WritingStyle.FORMAL)
            assertThat(ctx.promptHint).contains("professional")
        }
    }

    @Test
    fun notes_and_docs_packages_classified_correctly() {
        val docs = listOf(
            "notion.id",
            "md.obsidian",
            "com.google.android.apps.docs.editors.docs",
            "com.google.android.keep",
            "com.samsung.android.app.notes",
        )
        for (pkg in docs) {
            val ctx = AppContextEngine.detect(pkg, null)
            assertThat(ctx.category).isEqualTo(AppCategory.DOCS_NOTES)
            assertThat(ctx.promptHint).contains("structured")
        }
    }

    @Test
    fun dev_and_terminal_packages_classified_correctly() {
        val dev = listOf(
            "com.termux",
            "com.github.android",
            "io.github.gitjournal",
        )
        for (pkg in dev) {
            val ctx = AppContextEngine.detect(pkg, null)
            assertThat(ctx.category).isEqualTo(AppCategory.DEV_TERMINAL)
            assertThat(ctx.promptHint).contains("code")
        }
    }

    @Test
    fun ai_and_search_packages_classified_correctly() {
        val search = listOf(
            "com.openai.chatgpt",
            "ai.perplexity.app.android",
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.brave.browser",
        )
        for (pkg in search) {
            val ctx = AppContextEngine.detect(pkg, null)
            assertThat(ctx.category).isEqualTo(AppCategory.AI_SEARCH)
            assertThat(ctx.promptHint).contains("query")
        }
    }

    @Test
    fun unknown_package_falls_back_to_general() {
        val ctx = AppContextEngine.detect("com.random.unknownapp", null)
        assertThat(ctx.category).isEqualTo(AppCategory.GENERAL)
    }

    @Test
    fun field_hint_refines_context() {
        val ctx = AppContextEngine.detect("com.unknown.app", "Search or type URL")
        assertThat(ctx.category).isEqualTo(AppCategory.AI_SEARCH)
    }
}
