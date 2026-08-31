package app.openflow.docs

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** Guard against stale docs — version, NDK, targetSdk, links. */
class DocsStaleScanTest {

    private val root = UiSourceScan.projectRoot()

    private fun gradleText(): String = File(root, "app/build.gradle.kts").readText()
    private fun changelog(): String = File(root, "CHANGELOG.md").readText()

    @Test
    fun versionName_in_changelog() {
        val vn = Regex("""versionName\s*=\s*"([^"]+)"""").find(gradleText())?.groupValues?.get(1)
            ?: error("versionName not found")
        assertWithMessage("CHANGELOG.md missing ## $vn")
            .that(changelog())
            .contains("## $vn")
    }

    @Test
    fun targetSdk_36_in_build_and_testing() {
        val gradle = gradleText()
        assertThat(gradle).contains("targetSdk = 36")
        assertThat(gradle).contains("compileSdk = 36")
        val testing = File(root, "docs/testing.md").readText()
        assertWithMessage("docs/testing.md should mention target 36 or play-check")
            .that(testing.lowercase())
            .contains("36")
    }

    @Test
    fun ndk_16kb_in_docs() {
        val gradle = gradleText()
        assertThat(gradle).contains("28.2.13676358")
        val testing = File(root, "docs/testing.md").readText()
        assertThat(testing).contains("28.2.13676358")
        val cmake = File(root, "app/src/main/cpp/CMakeLists.txt").readText()
        assertThat(cmake).contains("16384")
    }

    @Test
    fun privacy_links_exist() {
        assertThat(File(root, "docs/PRIVACY.md").isFile).isTrue()
        assertThat(File(root, "docs/privacy.html").isFile).isTrue()
        val readme = File(root, "README.md").readText()
        // README should link to testing gate
        assertThat(readme).contains("testing.md")
    }

    @Test
    fun store_listing_lengths() {
        val title = File(root, "docs/store/en-US/title.txt")
        if (title.isFile) {
            val t = title.readText().trim()
            assertWithMessage("title >30").that(t.length).isAtMost(30)
        }
        val short = File(root, "docs/store/en-US/short_desc.txt")
        if (short.isFile) {
            val s = short.readText().trim()
            assertWithMessage("short >80").that(s.length).isAtMost(80)
        }
    }

    @Test
    fun no_coauthored_in_recent_commits_doc() {
        // Sanity: docs should not promise Co-Authored-By
        val contributing = File(root, "CONTRIBUTING.md").readText()
        assertThat(contributing).contains("No Co-Authored-By")
    }

    @Test
    fun tasks_todos_do_not_claim_done_without_marker() {
        // tasks/todo-*.md checkboxes may exist, but a task file that is fully
        // checked must be renamed/archived — pins "no zombie task lists".
        File(root, "tasks").listFiles { f -> f.name.startsWith("todo-") && f.extension == "md" }
            ?.forEach { f ->
                val text = f.readText()
                val boxes = Regex("""^\s*- \[.\]""", RegexOption.MULTILINE).findAll(text).toList()
                val unchecked = boxes.count { !it.value.contains("x") }
                assertWithMessage("${f.name} all done — archive or add next tasks")
                    .that(unchecked)
                    .isGreaterThan(0)
            }
    }

    @Test
    fun readme_splits_dev_vs_launch() {
        val readme = File(root, "README.md").readText()
        assertThat(readme).contains("## Dev vs Launch")
        assertThat(readme).contains("of_win")
        assertThat(readme).contains("release.yml")
        assertThat(readme).contains("No `Co-Authored-By`")
    }

    @Test
    fun setup_extracted_composables_exist() {
        assertThat(File(root, "app/src/main/java/app/openflow/ui/setup/SetupProgressDots.kt").isFile).isTrue()
        assertThat(File(root, "app/src/main/java/app/openflow/ui/setup/SetupStepCard.kt").isFile).isTrue()
        assertThat(File(root, "app/src/main/java/app/openflow/ui/setup/OemBatteryHint.kt").isFile).isTrue()
    }

    @Test
    fun language_catalog_floor_matches_beat_wispr() {
        val src = File(root, "app/src/main/java/app/openflow/stt/LanguagePolicy.kt").readText()
        val count = Regex("""LanguageOption\(""").findAll(src).count()
        assertWithMessage("LanguagePolicy catalog must stay ≥40 (beat-wispr grow)")
            .that(count)
            .isAtLeast(40)
    }

    @Test
    fun comparison_table_covers_wispr_column() {
        // Beating Wispr needs the comparison table kept current.
        val cmp = File(root, "docs/COMPARISON.md").readText()
        assertThat(cmp).contains("Wispr Flow")
        assertThat(cmp).contains("Keep your keyboard")
    }

    @Test
    fun guide_bubble_claims_match_prefs_defaults() {
        // GUIDE.md mentions opacity — keep defaults honest after pref changes.
        val guide = File(root, "docs/GUIDE.md").readText()
        assertThat(guide).contains("opacity")
        val prefs = File(root, "app/src/main/java/app/openflow/prefs/FlowPrefs.kt").readText()
        assertWithMessage("bubble default opacity must stay solid 1.00")
            .that(prefs)
            .contains("getFloat(\"bubble_opacity\", 1.00f)")
    }
}
