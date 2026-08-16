package app.openflow.ui.qa

import java.io.File

/** Locates ui source from Gradle cwd (app/ or repo root). JVM only. */
internal object UiSourceScan {
    fun projectRoot(): File {
        val userDir = System.getProperty("user.dir") ?: "."
        var dir = File(userDir).canonicalFile
        repeat(8) {
            if (File(dir, "app/src/main/java/app/openflow/ui/shell/AppRoute.kt").isFile) {
                return dir
            }
            if (File(dir, "src/main/java/app/openflow/ui/shell/AppRoute.kt").isFile) {
                return dir.parentFile
            }
            dir = dir.parentFile ?: return@repeat
        }
        error("open-flow root not found from $userDir")
    }

    fun uiKtText(): String {
        val ui = File(projectRoot(), "app/src/main/java/app/openflow/ui")
        require(ui.isDirectory) { "missing $ui" }
        return ui.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
    }

    fun valuesFile(name: String): File {
        val f = File(projectRoot(), "app/src/main/res/values/$name")
        require(f.isFile) { "missing $f" }
        return f
    }

    fun hasQuotedTag(source: String, tag: String): Boolean = source.contains("\"$tag\"")
}
