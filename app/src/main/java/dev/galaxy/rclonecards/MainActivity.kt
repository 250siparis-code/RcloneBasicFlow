package dev.galaxy.rclonecards

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.galaxy.rclonecards.data.CardStore
import dev.galaxy.rclonecards.data.ConfigManager
import dev.galaxy.rclonecards.data.FullBackupManager
import dev.galaxy.rclonecards.engine.DriveConnector
import dev.galaxy.rclonecards.engine.ShortcutIconFactory
import dev.galaxy.rclonecards.model.TaskCard
import dev.galaxy.rclonecards.service.RcloneService
import dev.galaxy.rclonecards.service.TaskScheduler
import dev.galaxy.rclonecards.ui.RcloneCardsRoot
import dev.galaxy.rclonecards.ui.RcloneCardsTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_RUN_CARD_ID = "run_card_id"
        const val EXTRA_OPEN_DETAIL_CARD_ID = "open_detail_card_id"
    }

    private val externalNavigation = MutableStateFlow<String?>(null)

    private val importConfigLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            ConfigManager.importFromUri(this, uri)
                .onSuccess { Toast.makeText(this, "rclone.conf imported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this, "Config could not be read: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private val exportConfigLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            ConfigManager.exportToUri(this, uri)
                .onSuccess { Toast.makeText(this, "rclone.conf exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this, "Config could not be written: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }


    private val importCardsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val oldIds = CardStore.cards.value.map { it.id }
            val result = runCatching {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("File could not be read")
                CardStore.importJson(raw).getOrThrow()
            }
            result.onSuccess { count ->
                oldIds.forEach { TaskScheduler.cancel(this, it) }
                CardStore.cards.value.forEach { TaskScheduler.apply(this, it.id) }
                Toast.makeText(this, "$count cards restored", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(this, "Card backup could not be opened: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val exportCardsLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val result = runCatching {
                contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(CardStore.exportJson()) }
                    ?: error("File could not be written")
            }
            Toast.makeText(
                this,
                if (result.isSuccess) "Card backup saved" else "Backup could not be saved: ${result.exceptionOrNull()?.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val importFullBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val oldIds = CardStore.cards.value.map { it.id }
            val result = runCatching {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Backup file could not be read")
                FullBackupManager.importJson(this, raw).getOrThrow()
            }
            result.onSuccess { count ->
                oldIds.forEach { TaskScheduler.cancel(this, it) }
                CardStore.cards.value.forEach { TaskScheduler.apply(this, it.id) }
                Toast.makeText(this, "$count cards and app settings restored", Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                Toast.makeText(this, "Full backup could not be restored: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val exportFullBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val result = runCatching {
                contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(FullBackupManager.exportJson())
                } ?: error("Backup file could not be written")
            }
            Toast.makeText(
                this,
                if (result.isSuccess) "Full app backup saved" else "Full backup failed: ${result.exceptionOrNull()?.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            RcloneCardsTheme {
                RcloneCardsRoot(
                    externalNavigation = externalNavigation,
                    onImportConfig = { importConfigLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) },
                    onExportConfig = { exportConfigLauncher.launch("rclone.conf") },
                    onConnectGoogleDrive = { remoteName, clientId, clientSecret ->
                        DriveConnector.connect(this, remoteName, clientId, clientSecret) { result ->
                            result.onSuccess { name ->
                                Toast.makeText(this, "$name connected", Toast.LENGTH_LONG).show()
                            }.onFailure { error ->
                                Toast.makeText(this, "Google Drive connection failed: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onImportCards = { importCardsLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    onExportCards = { exportCardsLauncher.launch("rclone-cards-backup.json") },
                    onImportFullBackup = { importFullBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    onExportFullBackup = { exportFullBackupLauncher.launch("rclone-cards-full-backup.json") },
                    onRequestAllFiles = { requestAllFilesAccess() },
                    onRequestNotification = { requestNotificationPermission() },
                    onRequestExactAlarm = { requestExactAlarmPermission() },
                    hasAllFilesAccess = { hasAllFilesAccess() },
                    hasNotificationPermission = { hasNotificationPermission() },
                    canExactAlarm = { canScheduleExactAlarms() },
                    onPinShortcut = { pinShortcut(it) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val runId = intent?.getStringExtra(EXTRA_RUN_CARD_ID)
        if (!runId.isNullOrBlank()) {
            startRclone(runId)
            externalNavigation.value = "detail:$runId"
            intent.removeExtra(EXTRA_RUN_CARD_ID)
            return
        }
        val detailId = intent?.getStringExtra(EXTRA_OPEN_DETAIL_CARD_ID)
        if (!detailId.isNullOrBlank()) {
            externalNavigation.value = "detail:$detailId"
            intent.removeExtra(EXTRA_OPEN_DETAIL_CARD_ID)
        }
    }

    private fun startRclone(cardId: String) {
        val serviceIntent = Intent(this, RcloneService::class.java).apply {
            action = RcloneService.ACTION_START
            putExtra(RcloneService.EXTRA_CARD_ID, cardId)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
    }

    private fun pinShortcut(card: TaskCard) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        if (!shortcutManager.isRequestPinShortcutSupported) return

        val launch = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_RUN_CARD_ID, card.id)
        }
        val shortcut = ShortcutInfo.Builder(this, "rclone-${card.id}")
            .setShortLabel(card.title.take(20))
            .setLongLabel("${card.title} · ${card.actionLabel}".take(60))
            .setIcon(ShortcutIconFactory.create(card))
            .setIntent(launch)
            .build()

        shortcutManager.requestPinShortcut(shortcut, null)
    }
}
