package app.openflow.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.openflow.R
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.ui.MainActivity

/**
 * Posts a notification when a dictation is saved.
 * Channel: "dictation" — shown briefly, no sound by default.
 */
object DictationNotifier {
    private const val CHANNEL_ID = "dictation_done"
    private const val NOTIF_ID = 1

    fun createChannel(ctx: Context): Boolean {
        return try {
            val desc = "Dictation saved"
            val ch = NotificationChannel(CHANNEL_ID, "Dictations", NotificationManager.IMPORTANCE_LOW).apply {
                this.description = desc
                setShowBadge(false)
            }
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            nm.createNotificationChannel(ch)
            true
        } catch (e: Exception) {
            android.util.Log.w("DictationNotifier", "createChannel failed", e)
            false
        }
    }

    /** true only if the notification was posted. false = no perm / fail. Never crash. */
    fun notifyIfPermitted(ctx: Context, wordCount: Int): Boolean {
        if (!canPost(ctx)) return false
        return try {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            val openIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_history", true)
            }
            val pending = PendingIntent.getActivity(
                ctx, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val text = if (wordCount > 0) "$wordCount words saved" else "Dictation saved"
            val copyPi = PendingIntent.getBroadcast(
                ctx, 1,
                Intent(FlowAccessibilityService.ACTION_COPY_LAST).setPackage(ctx.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle("Open Flow")
                .setContentText(text)
                .setContentIntent(pending)
                .addAction(0, "Copy last", copyPi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            nm.notify(NOTIF_ID, n)
            true
        } catch (e: Exception) {
            android.util.Log.w("DictationNotifier", "notifyIfPermitted failed", e)
            false
        }
    }

    /** true only if posted. false = no perm / fail. Never crash. */
    fun notifyServiceStopped(ctx: Context): Boolean {
        if (!canPost(ctx)) return false
        return try {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            val open = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            val pending = PendingIntent.getActivity(
                ctx, 2, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle("Open Flow")
                .setContentText("Flow Bubble stopped — tap to reopen Accessibility")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(NOTIF_ID + 1, n)
            true
        } catch (e: Exception) {
            android.util.Log.w("DictationNotifier", "notifyServiceStopped failed", e)
            false
        }
    }

    private fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Drop dictation + service-stopped notifications. */
    fun cancelAll(ctx: Context) {
        try {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.cancel(NOTIF_ID)
            nm.cancel(NOTIF_ID + 1)
        } catch (_: Exception) {
        }
    }
}
