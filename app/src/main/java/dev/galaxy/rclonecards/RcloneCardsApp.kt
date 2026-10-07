package dev.galaxy.rclonecards

import android.app.Application
import dev.galaxy.rclonecards.data.AppSettings
import dev.galaxy.rclonecards.data.CardStore
import dev.galaxy.rclonecards.data.ConfigManager
import dev.galaxy.rclonecards.service.JobRepository

class RcloneCardsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        CardStore.init(this)
        ConfigManager.init(this)
        JobRepository.init(this)
    }
}
