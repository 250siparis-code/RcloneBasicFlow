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
import dev.galaxy.rclonecards.data.ConfigManager
import dev.galaxy.rclonecards.data.JobHistoryStore
import dev.galaxy.rclonecards.engine.RcloneEngine
import dev.galaxy.rclonecards.engine.TransferProgress
import dev.galaxy.rclonecards.engine.TransferCompletionLedger
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
    private val startingJobs = ConcurrentHashMap.newKeySet<String>()
    private val completions = ConcurrentHashMap<String, TransferCompletionLedger>()
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
        if (processes.containsKey(cardId) || startingJobs.contains(cardId)) return
        val card = CardStore.get(cardId) ?: return
        if (!RcloneEngine.isBinaryAvailable(this)) {
            JobRepository.put(
                JobState(
                    cardId = card.id,
                    title = card.title,
                    status = JobStatus.ERROR,
                    lastError = "librclone.so was not found. The APK was built without the rclone engine."
                )
            )
            updateForeground()
            return
        }

        val missingRemotes = RcloneEngine.referencedRemotes(card.command)
            .filterNot { ConfigManager.hasRemote(it) }

        if (missingRemotes.isNotEmpty()) {
            val missing = missingRemotes.joinToString(", ")
            val message = "Remote '$missing' is not configured. Open Settings > Google Drive Setup or import rclone.conf."
            JobRepository.put(
                JobState(
                    cardId = card.id,
                    title = card.title,
                    status = JobStatus.ERROR,
                    lastError = message,
                    logs = listOf("ERROR  $message")
                )
            )
            updateForeground()
            return
        }

        val args = runCatching { RcloneEngine.buildProcessArgs(this, card.command, card.transfers, card.checkers, card.bwlimit) }.getOrElse { error ->
            JobRepository.put(
                JobState(card.id, card.title, JobStatus.ERROR, lastError = error.message ?: "Command could not be parsed")
            )
            updateForeground()
            return
        }

        if (!startingJobs.add(cardId)) return
        completions[cardId] = TransferCompletionLedger()
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
                    "echo \$\$ > \"\$1\"; shift; exec \"\$@\"",
                    "rclonecards",
                    pidFile.absolutePath
                ).apply {
                    addAll(args)
                }

                val builder = ProcessBuilder(shellArgs)
                    .redirectErrorStream(false)

                builder.environment().apply {
                    put("HOME", filesDir.absolutePath)
                    put("TMPDIR", cacheDir.absolutePath)
                    put("XDG_CACHE_HOME", cacheDir.absolutePath)
                }

                val process = builder.start()
                processes[cardId] = process
                if (JobRepository.get(cardId)?.status == JobStatus.STOPPED) process.destroyForcibly()

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
                    JobRepository.update(cardId) { it.copy(exitCode = exit, finishedAtMillis = it.finishedAtMillis ?: System.currentTimeMillis()) }
                } else if (exit == 0) {
                    JobRepository.update(cardId) {
                        it.copy(
                            status = JobStatus.COMPLETED,
                            progressPercent = 100,
                            exitCode = exit,
                            finishedAtMillis = System.currentTimeMillis()
                        )
                    }
                } else {
                    val fallbackError = previous?.lastError
                        ?: previous?.logs
                            ?.asReversed()
                            ?.firstOrNull { line ->
                                line.startsWith("ERROR", ignoreCase = true) ||
                                    line.startsWith("ERR", ignoreCase = true)
                            }
                            ?.substringAfter("  ", missingDelimiterValue = "")
                            ?.takeIf { it.isNotBlank() }
                        ?: "rclone exit code: $exit"

                    JobRepository.update(cardId) {
                        it.copy(
                            status = JobStatus.ERROR,
                            exitCode = exit,
                            lastError = fallbackError,
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
                startingJobs.remove(cardId)
                completions.remove(cardId)
                recordHistory(cardId)
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
                handleLine(cardId, line, label)
            }
        }
    }

    private fun handleLine(cardId: String, raw: String, streamLabel: String) {
        if (raw.isBlank()) return
        var display = raw
        var parsedJson = false
        var explicitError: String? = null

        runCatching {
            val obj = JSONObject(raw)
            parsedJson = true
            val level = obj.optString("level", "info")
            val msg = obj.optString("msg", raw).trim()
            val objectName = obj.optString("object", "")
            completions[cardId]?.record(obj)?.let { (files, bytes) ->
                JobRepository.update(cardId) { it.copy(confirmedFiles = files, confirmedBytes = bytes) }
            }
            display = buildString {
                append(level.uppercase())
                append("  ")
                if (objectName.isNotBlank()) {
                    append(objectName)
                    append("  ·  ")
                }
                append(msg.replace('\n', ' '))
            }

            // Rclone versions also emit progress as JSON messages rather than a stats object.
            // Parse the human-readable one-line stats as a fallback.
            if (obj.optJSONObject("stats") == null) parseProgressText(cardId, msg)
            val stats = obj.optJSONObject("stats")
            if (stats != null) {
                JobRepository.update(cardId) { TransferProgress.fromStats(it, stats) }
            }

            if (level.equals("error", true)) {
                explicitError = msg.ifBlank { raw }
            }
        }

        if (!parsedJson) {
            parseProgressText(cardId, raw)
            val isError = Regex("(?i)(error|fatal|failed|permission denied|unauthorized|not found)").containsMatchIn(raw)
            if (isError) {
                explicitError = raw.trim()
                display = "ERROR  ${raw.trim()}"
            } else {
                display = "INFO  ${raw.trim()}"
            }
        }

        explicitError?.takeIf { it.isNotBlank() }?.let { message ->
            JobRepository.update(cardId) { it.copy(lastError = message.take(1000)) }
        }

        JobRepository.appendLog(cardId, display.take(1800))
        updateForeground()
    }

    private fun parseProgressText(cardId: String, message: String) {
        // Matches e.g. "Transferred: 42.13 MiB / 52.13 MiB, 81%" and
        // "Transferred: 2 / 3, 66%"; never mistake a single file's 100% for job completion.
        val clean = message.replace(Regex("""\[[;\d]*[ -/]*[@-~]"""), "").replace('\r', ' ')
        val pct = Regex("""(?i)Transferred:\s*[^\n]*?[,\s]+(\d{1,3})%""").find(clean)
            ?.groupValues?.getOrNull(1)?.toIntOrNull()
        val bytes = Regex("""(?i)Transferred:\s*([0-9.]+)\s*(B|KiB|MiB|GiB|TiB)\s*/\s*([0-9.]+)\s*(B|KiB|MiB|GiB|TiB)""").find(clean)
        val counts = Regex("""(?i)Transferred:\s*(\d+)\s*/\s*(\d+)\s*,\s*(\d{1,3})%""").find(clean)
        if (pct == null && bytes == null && counts == null) return
        fun amount(v: String, unit: String): Long {
            val mult = when (unit.lowercase()) { "kib" -> 1024.0; "mib" -> 1048576.0; "gib" -> 1073741824.0; "tib" -> 1099511627776.0; else -> 1.0 }
            return ((v.toDoubleOrNull() ?: 0.0) * mult).toLong()
        }
        JobRepository.update(cardId) { state ->
            val running = state.status == JobStatus.RUNNING || state.status == JobStatus.PAUSED
            val p = (pct ?: counts?.groupValues?.get(3)?.toIntOrNull() ?: state.progressPercent).coerceIn(0, if (running) 99 else 100)
            state.copy(progressPercent = maxOf(state.progressPercent, p),
                bytes = bytes?.let { amount(it.groupValues[1], it.groupValues[2]) } ?: state.bytes,
                totalBytes = bytes?.let { amount(it.groupValues[3], it.groupValues[4]) } ?: state.totalBytes,
                transfers = counts?.groupValues?.get(1)?.toLongOrNull() ?: state.transfers,
                totalTransfers = counts?.groupValues?.get(2)?.toLongOrNull() ?: state.totalTransfers)
        }
    }

    private fun pauseJob(cardId: String) {
        if (!processes.containsKey(cardId)) return

        val pid = processIds[cardId]
        if (pid == null) {
            JobRepository.appendLog(cardId, "ERROR  Process PID was not found")
            return
        }

        runCatching {
            Os.kill(pid, OsConstants.SIGSTOP)
            JobRepository.update(cardId) { it.copy(status = JobStatus.PAUSED) }
            JobRepository.appendLog(cardId, "PAUSED  Task paused")
            updateForeground()
        }.onFailure {
            JobRepository.appendLog(cardId, "ERROR  Pause failed: ${it.message}")
        }
    }

    private fun resumeJob(cardId: String) {
        if (!processes.containsKey(cardId)) return

        val pid = processIds[cardId]
        if (pid == null) {
            JobRepository.appendLog(cardId, "ERROR  Process PID was not found")
            return
        }

        runCatching {
            Os.kill(pid, OsConstants.SIGCONT)
            JobRepository.update(cardId) { it.copy(status = JobStatus.RUNNING) }
            JobRepository.appendLog(cardId, "INFO  Task resumed")
            updateForeground()
        }.onFailure {
            JobRepository.appendLog(cardId, "ERROR  Resume failed: ${it.message}")
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
        JobRepository.appendLog(cardId, "STOP  Task stopped by user")
        updateForeground()
        maybeStopSelf()
    }

    private fun enqueue(cardId: String) {
        if (processes.containsKey(cardId) || startingJobs.contains(cardId)) return
        val card = CardStore.get(cardId) ?: return
        synchronized(queueLock) {
            if (!queue.contains(cardId) && !processes.containsKey(cardId)) queue.addLast(cardId)
        }
        JobRepository.put(JobState(card.id, card.title, JobStatus.QUEUED, logs = listOf("Added to queue")))
        updateForeground()
        pumpQueue()
    }

    private fun pumpQueue() {
        if (processes.isNotEmpty() || startingJobs.isNotEmpty()) return
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
        val title = active?.title ?: "Basic Rclone Flow"
        val knownProgress = active != null && (active.totalBytes > 0L || active.totalTransfers > 0L)
        val statusText = when (active?.status) {
            JobStatus.RUNNING -> if (knownProgress) "Running · ${active.progressPercent}% · ${formatSpeed(active.speedBytesPerSecond)}"
                else "Preparing · ${formatSpeed(active.speedBytesPerSecond)}"
            JobStatus.PAUSED -> if (knownProgress) "Paused · ${active.progressPercent}%" else "Paused"
            JobStatus.QUEUED -> "Queued"
            else -> "No active transfer"
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(openPending)
            .setOnlyAlertOnce(true)
            .setOngoing(active != null)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (active != null && active.status != JobStatus.QUEUED) {
            builder.setProgress(100, active.progressPercent, !knownProgress && active.status == JobStatus.RUNNING)
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
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
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
            "Completed · ${formatBytes(state.bytes)} · ${state.transfers} files"
        } else {
            state.lastError ?: "rclone finished with an error"
        }
        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
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

    private fun recordHistory(cardId: String) {
        val state=JobRepository.get(cardId) ?: return
        if(state.status in setOf(JobStatus.COMPLETED,JobStatus.ERROR,JobStatus.STOPPED)) {
            JobHistoryStore.record(state,CardStore.get(cardId)?.command.orEmpty())
        }
    }

    private fun maybeStopSelf() {
        val anyActive = JobRepository.jobs.value.values.any {
            it.status == JobStatus.RUNNING || it.status == JobStatus.PAUSED || it.status == JobStatus.QUEUED
        }
        val hasQueued = synchronized(queueLock) { queue.isNotEmpty() }
        if (!anyActive && !hasQueued && processes.isEmpty() && startingJobs.isEmpty()) {
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
                "Basic Rclone Flow transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background rclone tasks"
                setShowBadge(false)
            }
            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                "Basic Rclone Flow results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Completed or failed rclone tasks"
            }
            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(channel)
                createNotificationChannel(resultChannel)
            }
        }
    }

    private fun formatSpeed(bytesPerSecond: Double) = TransferProgress.formatSpeed(bytesPerSecond)

    override fun onDestroy() {
        processes.values.forEach { runCatching { it.destroyForcibly() } }
        processes.clear()
        releaseWakeLock()
        super.onDestroy()
    }
}
