package dev.galaxy.rclonecards.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cardId = intent.getStringExtra(RcloneService.EXTRA_CARD_ID) ?: return
        val serviceIntent = Intent(context, RcloneService::class.java).apply {
            action = RcloneService.ACTION_START
            putExtra(RcloneService.EXTRA_CARD_ID, cardId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        // Günlük alarmı bir sonraki güne yeniden kur.
        TaskScheduler.apply(context, cardId)
    }
}
