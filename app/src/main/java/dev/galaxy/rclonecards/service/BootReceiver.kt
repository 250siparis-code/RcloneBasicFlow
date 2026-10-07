package dev.galaxy.rclonecards.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.galaxy.rclonecards.data.CardStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        CardStore.init(context)
        CardStore.cards.value.filter { it.scheduleEnabled }.forEach { TaskScheduler.apply(context, it.id) }
    }
}
