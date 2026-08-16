package app.openflow.insights

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/** Brutal local share card — cream/charcoal, no cloud. */
object InsightShareCard {
    fun render(
        totalWords: Long,
        totalSessions: Long,
        streakDays: Int,
        wpm: Double,
        widthPx: Int = 1080,
        heightPx: Int = 1350,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cream = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5F0E8.toInt() }
        val charcoal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A1A1A.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A1A1A.toInt()
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A1A1A.toInt()
            textSize = 48f
        }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5C5C5C.toInt()
            textSize = 36f
        }
        c.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), cream)
        c.drawRect(24f, 24f, widthPx - 24f, heightPx - 24f, charcoal)
        var y = 160f
        c.drawText("Open Flow", 80f, y, title)
        y += 100f
        c.drawText("My voice stats", 80f, y, muted)
        y += 140f
        c.drawText("Words  $totalWords", 80f, y, body)
        y += 90f
        c.drawText("Sessions  $totalSessions", 80f, y, body)
        y += 90f
        c.drawText("Streak  $streakDays days", 80f, y, body)
        y += 90f
        c.drawText("Speed  ${"%.0f".format(wpm)} WPM", 80f, y, body)
        y += 160f
        c.drawText("Local on my phone. No cloud.", 80f, y, muted)
        return bmp
    }
}
