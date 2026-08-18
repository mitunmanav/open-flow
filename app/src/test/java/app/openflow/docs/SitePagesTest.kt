package app.openflow.docs

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** GitHub Pages HTML: same nav, honest INTERNET, no filler words. */
class SitePagesTest {

    private val pages = File(UiSourceScan.projectRoot(), "docs")
        .listFiles()
        .orEmpty()
        .filter { it.extension == "html" }

    @Test
    fun pages_exist() {
        val names = pages.map { it.name }.toSet()
        assertThat(names).containsAtLeast(
            "index.html",
            "install.html",
            "guide.html",
            "privacy.html",
            "report.html",
            "compare.html",
            "architecture.html",
            "roadmap.html",
        )
    }

    @Test
    fun every_page_has_talk_and_privacy_nav() {
        pages.forEach { f ->
            val t = f.readText()
            assertWithMessage("${f.name} missing Privacy nav")
                .that(t)
                .contains("privacy.html")
            assertWithMessage("${f.name} missing Talk / Discussions")
                .that(t)
                .contains("github.com/mitunmanav/open-flow/discussions")
        }
    }

    @Test
    fun no_filler_or_stale_lines() {
        val banned = listOf(
            "moat",
            "product shell",
            "no sugar",
            "inspectable",
            "copy / undo",
            "adding internet",
            "does not declare internet",
            "no internet permission",
            "seamless",
            "leverage",
            "empower",
            "robust",
            "streamline",
            "cutting-edge",
            "game-changer",
            "delve",
            "utilize",
        )
        pages.forEach { f ->
            val low = f.readText().lowercase()
            banned.forEach { word ->
                assertWithMessage("${f.name} has \"$word\"")
                    .that(low)
                    .doesNotContain(word)
            }
        }
    }

    @Test
    fun index_and_privacy_say_internet_declared() {
        val index = File(UiSourceScan.projectRoot(), "docs/index.html").readText().lowercase()
        val privacy = File(UiSourceScan.projectRoot(), "docs/privacy.html").readText().lowercase()
        assertThat(index).contains("internet is declared")
        assertThat(privacy).contains("internet is declared")
        assertThat(index).doesNotContain("ime")
    }
}
