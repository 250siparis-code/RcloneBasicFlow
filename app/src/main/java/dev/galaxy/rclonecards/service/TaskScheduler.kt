package dev.galaxy.rclonecards.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.galaxy.rclonecards.data.CardStore
import java.util.Calendar

object TaskScheduler {
    fun apply(context: Context, cardId: String) {
        val card = CardStore.get(cardId) ?: return
        cancel(context, cardId)
        if (!card.scheduleEnabled || card.scheduleHour == null || card.scheduleMinute == null) return

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, card.scheduleHour)
            set(Calendar.MINUTE, card.scheduleMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, cardId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pi)
        } else {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pi)
        }
    }

    fun cancel(context: Context, cardId: String) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(pendingIntent(context, cardId))
    }

    private fun pendingIntent(context: Context, cardId: String): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "dev.galaxy.rclonecards.ALARM"
            putExtra(RcloneService.EXTRA_CARD_ID, cardId)
        }
        return PendingIntent.getBroadcast(
            context,
            cardId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
