package dev.galaxy.rclonecards.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.galaxy.rclonecards.MainActivity
import dev.galaxy.rclonecards.R

/** Keeps the app process eligible to finish the loopback OAuth exchange in the browser. */
class DriveSetupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("drive_setup", "Google Drive setup", NotificationManager.IMPORTANCE_LOW))
        val open = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("basic-rclone-flow://oauth?status=return")
        }
        val notification = NotificationCompat.Builder(this, "drive_setup")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Google Drive setup")
            .setContentText("Return to Basic Rclone Flow to continue setup")
            .setContentIntent(PendingIntent.getActivity(this, 7102, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 7102, notification,
            if (android.os.Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0)
        return START_NOT_STICKY
    }
}
