package app.openflow.ui.style

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class LauncherApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

object LauncherAppQuery {
    fun list(pm: PackageManager): List<LauncherApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName?.trim().orEmpty()
            if (pkg.isEmpty()) return@mapNotNull null
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
            LauncherApp(pkg, label, icon)
        }
            .distinctBy { it.packageName.lowercase() }
            .sortedBy { it.label.lowercase() }
    }

    fun filter(apps: List<LauncherApp>, query: String): List<LauncherApp> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps
        return apps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }
}
