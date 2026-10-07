package dev.galaxy.rclonecards.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.system.Os
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.galaxy.rclonecards.MainActivity
import dev.galaxy.rclonecards.R
import dev.galaxy.rclonecards.data.AppSettings
import dev.galaxy.rclonecards.data.CardStore
import dev.galaxy.rclonecards.engine.RcloneEngine
import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class RcloneService : Service() {

    companion object {
        const val ACTION_START = "dev.galaxy.rclonecards.START"
        const val ACTION_ENQUEUE = "dev.galaxy.rclonecards.ENQUEUE"
        const val ACTION_STOP = "dev.galaxy.rclonecards.STOP"
        const val ACTION_PAUSE = "dev.galaxy.rclonecards.PAUSE"
        const val ACTION_RESUME = "dev.galaxy.rclonecards.RESUME"
        const val EXTRA_CARD_ID = "card_id"

        private const val CHANNEL_ID = "rclone_transfers"
        private const val RESULT_CHANNEL_ID = "rclone_results"
        private const val NOTIFICATION_ID = 7101
    }

    private val processes = ConcurrentHashMap<String, Process>()
    private val processIds = ConcurrentHashMap<String, Int>()
    private val queue = ArrayDeque<String>()
    private val queueLock = Any()
    private val wakeLock: PowerManager.WakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:rclone-transfer")
            .apply { setReferenceCounted(false) }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        val cardId = intent?.getStringExtra(EXTRA_CARD_ID)
        when (intent?.action) {
            ACTION_START -> cardId?.let { startJob(it) }
            ACTION_ENQUEUE -> cardId?.let { enqueue(it) }
            ACTION_STOP -> cardId?.let { stopJob(it) }
            ACTION_PAUSE -> cardId?.let { pauseJob(it) }
            ACTION_RESUME -> cardId?.let { resumeJob(it) }
        }
        // Geçersiz/boş bir intent ile servis açılırsa foreground bildirimi takılı kalmasın.
        maybeStopSelf()
        return START_NOT_STICKY
    }

    private fun startJob(cardId: String) {
        if (processes.containsKey(cardId)) return
        val card = CardStore.get(cardId) ?: return
        if (!RcloneEngine.isBinaryAvailable(this)) {
            JobRepository.put(
                JobState(
                    cardId = card.id,
                    title = card.title,
                    status = JobStatus.ERROR,
                    lastError = "librclone.so bulunamadı. APK, rclone motoru eklenmeden derlenmiş."
                )
            )
            updateForeground()
            return
        }

        val args = runCatching { RcloneEngine.buildProcessArgs(this, card.command, card.transfers, card.checkers, card.bwlimit) }.getOrElse { error ->
            JobRepository.put(
                JobState(card.id, card.title, JobStatus.ERROR, lastError = error.message ?: "Komut ayrıştırılamadı")
            )
            updateForeground()
            return
        }

        acquireWakeLock()
        JobRepository.put(
            JobState(
                cardId = card.id,
                title = card.title,
                status = JobStatus.RUNNING,
                startedAtMillis = System.currentTimeMillis(),
                logs = listOf("$ ${card.command}")
            )
        )
        updateForeground()

        thread(name = "rclone-${card.id}") {
            try {
                val pidFile = java.io.File(cacheDir, "rclone-$cardId.pid").apply { delete() }

                val shellArgs = mutableListOf(
                    "/system/bin/sh",
                    "-c",
                    "echo \\$\\$ > \"\\$1\"; shift; exec \"\\$@\"",
                    "rclonecards",
                    pidFile.absolutePath
                ).apply {
                    addAll(args)
                }

                val builder = ProcessBuilder(shellArgs)
                    .directory(card.workDir.takeIf { it.isNotBlank() }?.let { java.io.File(it) })
                    .redirectErrorStream(false)

                builder.environment().apply {
                    put("HOME", filesDir.absolutePath)
                    put("TMPDIR", cacheDir.absolutePath)
                    put("XDG_CACHE_HOME", cacheDir.absolutePath)
                }

                val process = builder.start()
                processes[cardId] = process

                var childPid: Int? = null
                for (attempt in 0 until 40) {
                    childPid = runCatching {
                        if (pidFile.exists()) {
                            pidFile.readText().trim().toIntOrNull()
                        } else {
                            null
                        }
                    }.getOrNull()

                    if (childPid != null) break
                    Thread.sleep(25)
                }

                childPid?.let { processIds[cardId] = it }

                val stdoutThread = streamReader(cardId, process.inputStream.bufferedReader(), "OUT")
                val stderrThread = streamReader(cardId, process.errorStream.bufferedReader(), "ERR")

                val exit = process.waitFor()
                stdoutThread.join(1500)
                stderrThread.join(1500)
                processes.remove(cardId)
                processIds.remove(cardId)
                pidFile.delete()

                val previous = JobRepository.get(cardId)
                if (previous?.status == JobStatus.STOPPED) {
                    JobRepository.update(cardId) { it.copy(exitCode = exit, finishedAtMillis = System.currentTimeMillis()) }
                } else if (exit == 0) {
                    JobRepository.update(cardId) {
                        it.copy(
                            status = JobStatus.COMPLETED,
                            progressPercent = if (it.totalBytes > 0 || it.totalTransfers > 0) 100 else it.progressPercent,
                            exitCode = exit,
                            finishedAtMillis = System.currentTimeMillis()
                        )
                    }
                } else {
                    JobRepository.update(cardId) {
                        it.copy(
                            status = JobStatus.ERROR,
                            exitCode = exit,
                            lastError = it.lastError ?: "rclone hata kodu: $exit",
                            finishedAtMillis = System.currentTimeMillis()
                        )
                    }
                }
            } catch (t: Throwable) {
                processes.remove(cardId)
                processIds.remove(cardId)
                JobRepository.update(cardId) {
                    it.copy(
                        status = JobStatus.ERROR,
                        lastError = t.message ?: t.javaClass.simpleName,
                        finishedAtMillis = System.currentTimeMillis()
                    )
                }
            } finally {
                postResultNotification(cardId)
                updateForeground()
                pumpQueue()
                maybeStopSelf()
            }
        }
    }

    private fun streamReader(cardId: String, reader: BufferedReader, label: String): Thread = thread(
        name = "rclone-stream-$cardId-$label"
    ) {
        reader.useLines { lines ->
            lines.forEach { line ->
                handleLine(cardId, line)
            }
        }
    }

    private fun handleLine(cardId: String, raw: String) {
        if (raw.isBlank()) return
        var display = raw
        runCatching {
            val obj = JSONObject(raw)
            val level = obj.optString("level", "info")
            val msg = obj.optString("msg", raw).trim()
            val objectName = obj.optString("object", "")
            display = buildString {
                append(level.uppercase())
                append("  ")
                if (objectName.isNotBlank()) {
                    append(objectName)
                    append("  ·  ")
                }
                append(msg.replace('\n', ' '))
            }

            val stats = obj.optJSONObject("stats")
            if (stats != null) {
                val bytes = stats.optLong("bytes", 0L)
                val totalBytes = stats.optLong("totalBytes", 0L)
                val transfers = stats.optLong("transfers", 0L)
                val totalTransfers = stats.optLong("totalTransfers", 0L)
                val speed = stats.optDouble("speed", 0.0)
                val eta = if (stats.isNull("eta")) null else stats.optLong("eta")
                val elapsed = stats.optDouble("elapsedTime", 0.0)
                val errors = stats.optInt("errors", 0)
                val current = stats.optJSONArray("transferring")?.let { arr ->
                    if (arr.length() > 0) arr.optJSONObject(0)?.optString("name") else null
                }
                val percent = when {
                    totalBytes > 0 -> ((bytes.toDouble() / totalBytes.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
                    totalTransfers > 0 -> ((transfers.toDouble() / totalTransfers.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
                    else -> JobRepository.get(cardId)?.progressPercent ?: 0
                }
                JobRepository.update(cardId) {
                    it.copy(
                        progressPercent = percent,
                        bytes = bytes,
                        totalBytes = totalBytes,
                        transfers = transfers,
                        totalTransfers = totalTransfers,
                        speedBytesPerSecond = speed,
                        etaSeconds = eta,
                        elapsedSeconds = elapsed,
                        currentFile = current,
                        errors = errors
                    )
                }
            }

            if (level.equals("error", true)) {
                JobRepository.update(cardId) { it.copy(lastError = msg.ifBlank { raw }) }
            }
        }
        JobRepository.appendLog(cardId, display.take(1800))
        updateForeground()
    }

    private fun pauseJob(cardId: String) {
        if (!processes.containsKey(cardId)) return

        val pid = processIds[cardId]
        if (pid == null) {
            JobRepository.appendLog(cardId, "ERROR  İşlem PID bilgisi bulunamadı")
            return
        }

        runCatching {
            Os.kill(pid, OsConstants.SIGSTOP)
            JobRepository.update(cardId) { it.copy(status = JobStatus.PAUSED) }
            JobRepository.appendLog(cardId, "PAUSED  İşlem duraklatıldı")
            updateForeground()
        }.onFailure {
            JobRepository.appendLog(cardId, "ERROR  Duraklatılamadı: ${it.message}")
        }
    }

    private fun resumeJob(cardId: String) {
        if (!processes.containsKey(cardId)) return

        val pid = processIds[cardId]
        if (pid == null) {
            JobRepository.appendLog(cardId, "ERROR  İşlem PID bilgisi bulunamadı")
            return
        }

        runCatching {
            Os.kill(pid, OsConstants.SIGCONT)
            JobRepository.update(cardId) { it.copy(status = JobStatus.RUNNING) }
            JobRepository.appendLog(cardId, "INFO  İşlem devam ediyor")
            updateForeground()
        }.onFailure {
            JobRepository.appendLog(cardId, "ERROR  Devam ettirilemedi: ${it.message}")
        }
    }

    private fun stopJob(cardId: String) {
        val process = processes[cardId]
        JobRepository.update(cardId) {
            it.copy(status = JobStatus.STOPPED, lastError = null, finishedAtMillis = System.currentTimeMillis())
        }
        if (process != null) {
            runCatching {
                // Duraklatılmış bir işlemi önce devam ettir ki terminate sinyalini alabilsin.
                processIds[cardId]?.let { pid ->
                    Os.kill(pid, OsConstants.SIGCONT)
                }
            }
            process.destroy()
            thread {
                Thread.sleep(1200)
                if (process.isAlive) process.destroyForcibly()
            }
        }
        processIds.remove(cardId)
        synchronized(queueLock) { queue.remove(cardId) }
        JobRepository.appendLog(cardId, "STOP  İşlem kullanıcı tarafından durduruldu")
        updateForeground()
        maybeStopSelf()
    }

    private fun enqueue(cardId: String) {
        val card = CardStore.get(cardId) ?: return
        synchronized(queueLock) {
            if (!queue.contains(cardId) && !processes.containsKey(cardId)) queue.addLast(cardId)
        }
        JobRepository.put(JobState(card.id, card.title, JobStatus.QUEUED, logs = listOf("Sıraya eklendi")))
        updateForeground()
        pumpQueue()
    }

    private fun pumpQueue() {
        if (processes.isNotEmpty()) return
        val next = synchronized(queueLock) { if (queue.isEmpty()) null else queue.removeFirst() }
        if (next != null) startJob(next)
    }

    private fun ensureForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
    }

    private fun updateForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val active = JobRepository.jobs.value.values.firstOrNull {
            it.status == JobStatus.RUNNING || it.status == JobStatus.PAUSED || it.status == JobStatus.QUEUED
        }
        val title = active?.title ?: "Rclone Cards"
        val statusText = when (active?.status) {
            JobStatus.RUNNING -> "Çalışıyor · %${active.progressPercent} · ${formatSpeed(active.speedBytesPerSecond)}"
            JobStatus.PAUSED -> "Duraklatıldı · %${active.progressPercent}"
            JobStatus.QUEUED -> "Sırada"
            else -> "Aktif aktarım yok"
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            active?.cardId?.let { putExtra(MainActivity.EXTRA_OPEN_DETAIL_CARD_ID, it) }
        }
        val openPending = PendingIntent.getActivity(
            this,
            40,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(openPending)
            .setOnlyAlertOnce(true)
            .setOngoing(active != null)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (active != null && active.status != JobStatus.QUEUED) {
            builder.setProgress(100, active.progressPercent, active.totalBytes <= 0L && active.totalTransfers <= 0L)
            val stopIntent = Intent(this, RcloneService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_CARD_ID, active.cardId)
            }
            val stopPending = PendingIntent.getService(
                this,
                active.cardId.hashCode(),
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", stopPending)
        }
        return builder.build()
    }

    private fun postResultNotification(cardId: String) {
        if (!AppSettings.notifyOnCompletion.value) return
        val state = JobRepository.get(cardId) ?: return
        if (state.status != JobStatus.COMPLETED && state.status != JobStatus.ERROR) return

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_DETAIL_CARD_ID, cardId)
        }
        val openPending = PendingIntent.getActivity(
            this,
            5000 + (cardId.hashCode() and 0x0FFF),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ok = state.status == JobStatus.COMPLETED
        val body = if (ok) {
            "Tamamlandı · ${formatBytes(state.bytes)} · ${state.transfers} dosya"
        } else {
            state.lastError ?: "rclone işlemi hata ile sonlandı"
        }
        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setSmallIcon(if (ok) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error)
            .setContentTitle(state.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        getSystemService(NotificationManager::class.java).notify(9000 + (cardId.hashCode() and 0x0FFF), notification)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var i = 0
        while (value >= 1024.0 && i < units.lastIndex) {
            value /= 1024.0
            i++
        }
        return if (value >= 100 || i == 0) "%.0f %s".format(value, units[i]) else "%.1f %s".format(value, units[i])
    }

    private fun maybeStopSelf() {
        val anyActive = JobRepository.jobs.value.values.any {
            it.status == JobStatus.RUNNING || it.status == JobStatus.PAUSED || it.status == JobStatus.QUEUED
        }
        val hasQueued = synchronized(queueLock) { queue.isNotEmpty() }
        if (!anyActive && !hasQueued && processes.isEmpty()) {
            releaseWakeLock()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) wakeLock.acquire()
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rclone aktarımları",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda çalışan rclone görevleri"
                setShowBadge(false)
            }
            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                "Rclone sonuçları",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tamamlanan veya hata veren rclone görevleri"
            }
            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(channel)
                createNotificationChannel(resultChannel)
            }
        }
    }

    private fun formatSpeed(bytesPerSecond: Double): String {
        if (bytesPerSecond <= 0.0) return "0 B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var value = bytesPerSecond
        var i = 0
        while (value >= 1024.0 && i < units.lastIndex) {
            value /= 1024.0
            i++
        }
        return if (value >= 100 || i == 0) "%.0f %s".format(value, units[i]) else "%.1f %s".format(value, units[i])
    }

    override fun onDestroy() {
        processes.values.forEach { runCatching { it.destroyForcibly() } }
        processes.clear()
        releaseWakeLock()
        super.onDestroy()
    }
}
