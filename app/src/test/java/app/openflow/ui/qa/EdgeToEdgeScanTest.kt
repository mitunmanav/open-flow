package app.openflow.ui.qa

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class EdgeToEdgeScanTest {
    private fun root() = UiSourceScan.projectRoot()

    @Test
    fun main_activity_enables_edge_to_edge() {
        val main = File(root(), "app/src/main/java/app/openflow/ui/MainActivity.kt").readText()
        assertThat(main).contains("enableEdgeToEdge()")
    }

    @Test
    fun manifest_uses_adjust_resize() {
        val manifest = File(root(), "app/src/main/AndroidManifest.xml").readText()
        assertThat(manifest).contains("android:windowSoftInputMode=\"adjustResize\"")
    }

    @Test
    fun app_shell_uses_safe_drawing_insets() {
        val shell = File(root(), "app/src/main/java/app/openflow/ui/shell/AppShell.kt").readText()
        assertThat(shell).contains("contentWindowInsets = WindowInsets.safeDrawing")
    }

    @Test
    fun peeled_screens_do_not_import_enable_edge_to_edge() {
        val ui = File(root(), "app/src/main/java/app/openflow/ui")
        val offenders = ui.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "MainActivity.kt" }
            .filter { it.readText().contains("enableEdgeToEdge") }
            .map { it.name }
            .toList()
        assertThat(offenders).isEmpty()
    }

    @Test
    fun peeled_screens_do_not_import_back_handler() {
        val ui = File(root(), "app/src/main/java/app/openflow/ui")
        val offenders = ui.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "MainActivity.kt" }
            .filter { it.readText().contains("import androidx.activity.compose.BackHandler") }
            .map { it.name }
            .toList()
        assertThat(offenders).isEmpty()
    }
}
