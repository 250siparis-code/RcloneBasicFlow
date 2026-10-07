package dev.galaxy.rclonecards.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import dev.galaxy.rclonecards.model.CardColor
import dev.galaxy.rclonecards.model.CardIcon
import dev.galaxy.rclonecards.model.TaskCard
import kotlin.math.roundToInt

object ShortcutIconFactory {

    fun create(card: TaskCard): Icon {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val customBitmap = card.customIconPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }

        if (customBitmap != null) {
            val dst = RectF(28f, 22f, 164f, 170f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (card.iconAlpha.coerceIn(0.15f, 1f) * 255f).roundToInt()
            }
            canvas.drawBitmap(customBitmap, null, dst, paint)
            return Icon.createWithBitmap(bitmap)
        }

        val color = resolveColor(card)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        when (card.icon) {
            CardIcon.PHONE -> {
                canvas.drawRoundRect(RectF(62f, 34f, 130f, 158f), 13f, 13f, stroke)
                canvas.drawCircle(96f, 137f, 4f, fill)
            }
            CardIcon.CAMERA -> {
                canvas.drawRoundRect(RectF(42f, 62f, 150f, 138f), 16f, 16f, stroke)
                canvas.drawRect(67f, 50f, 99f, 67f, fill)
                canvas.drawCircle(96f, 100f, 24f, stroke)
            }
            CardIcon.FOLDER -> {
                canvas.drawRoundRect(RectF(34f, 64f, 158f, 140f), 14f, 14f, stroke)
                canvas.drawLine(43f, 64f, 43f, 52f, stroke)
                canvas.drawLine(43f, 52f, 88f, 52f, stroke)
                canvas.drawLine(88f, 52f, 100f, 64f, stroke)
            }
            CardIcon.CLOUD -> {
                canvas.drawCircle(72f, 101f, 27f, stroke)
                canvas.drawCircle(104f, 82f, 34f, stroke)
                canvas.drawCircle(131f, 104f, 23f, stroke)
                canvas.drawLine(58f, 127f, 137f, 127f, stroke)
            }
            CardIcon.DESKTOP -> {
                canvas.drawRoundRect(RectF(34f, 45f, 158f, 126f), 10f, 10f, stroke)
                canvas.drawLine(96f, 126f, 96f, 149f, stroke)
                canvas.drawLine(70f, 151f, 122f, 151f, stroke)
            }
            CardIcon.SETTINGS -> {
                canvas.drawCircle(96f, 96f, 30f, stroke)
                canvas.drawCircle(96f, 96f, 9f, fill)
                canvas.drawLine(96f, 42f, 96f, 59f, stroke)
                canvas.drawLine(96f, 133f, 96f, 150f, stroke)
                canvas.drawLine(42f, 96f, 59f, 96f, stroke)
                canvas.drawLine(133f, 96f, 150f, 96f, stroke)
                canvas.drawLine(58f, 58f, 70f, 70f, stroke)
                canvas.drawLine(122f, 122f, 134f, 134f, stroke)
                canvas.drawLine(134f, 58f, 122f, 70f, stroke)
                canvas.drawLine(70f, 122f, 58f, 134f, stroke)
            }
        }

        return Icon.createWithBitmap(bitmap)
    }

    private fun resolveColor(card: TaskCard): Int {
        val preset = when (card.color) {
            CardColor.GREEN -> Color.rgb(34, 197, 94)
            CardColor.PURPLE -> Color.rgb(168, 85, 247)
            CardColor.AMBER -> Color.rgb(245, 158, 11)
            CardColor.BLUE -> Color.rgb(6, 182, 212)
            CardColor.RED -> Color.rgb(239, 68, 68)
            CardColor.SLATE -> Color.rgb(115, 115, 115)
        }

        val base = runCatching {
            val raw = card.customColorHex.trim()
            if (raw.isBlank()) preset
            else Color.parseColor(if (raw.startsWith("#")) raw else "#$raw")
        }.getOrDefault(preset)

        val alpha = (card.iconAlpha.coerceIn(0.15f, 1f) * 255f).roundToInt()
        return Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
    }
}
