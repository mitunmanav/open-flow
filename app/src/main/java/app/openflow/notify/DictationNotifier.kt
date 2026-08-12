package app.openflow.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.openflow.R
import app.openflow.ui.MainActivity

/**
 * Posts a notification when a dictation is saved.
 * Channel: "dictation" — shown briefly, no sound by default.
 */
object DictationNotifier {
    private const val CHANNEL_ID = "dictation_done"
    private const val NOTIF_ID = 1

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val desc = "Dictation saved"
            val ch = NotificationChannel(CHANNEL_ID, "Dictations", NotificationManager.IMPORTANCE_LOW).apply {
                this.description = desc
                setShowBadge(false)
            }
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }

    fun notifyIfPermitted(ctx: Context, wordCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (wordCount > 0) "$wordCount words saved" else "Dictation saved"
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Open Flow")
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, n)
    }
}
