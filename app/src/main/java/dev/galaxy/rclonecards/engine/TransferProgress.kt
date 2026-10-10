package dev.galaxy.rclonecards.engine

import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

/** Global transfer statistics, never an individual file's percentage. */
object TransferProgress {
    fun awaitingConfirmation(state: JobState) = state.status == JobStatus.RUNNING &&
        state.totalBytes > 0 && state.bytes >= state.totalBytes && state.activeTransfers > 0

    /** Older engines can wrap a multiline stats report in a JSON message without stats. */
    fun fromText(state: JobState, message: String): JobState {
        val report = message.replace(Regex("\u001b\\[[;\\d]*[ -/]*[@-~]"), "").replace('\r', ' ')
        val amount = "([0-9.]+)\\s*(B|KiB|MiB|GiB|TiB|kBytes|MBytes|GBytes|TBytes)"
        val bytes = Regex("(?i)Transferred:\\s*$amount\\s*/\\s*$amount\\s*,\\s*(\\d{1,3})%\\s*,\\s*$amount/s\\s*,\\s*ETA\\s*([^\\s]+)").find(report)
        val files = Regex("(?i)Transferred:\\s*(\\d+)\\s*/\\s*(\\d+)\\s*,\\s*\\d{1,3}%").find(report)
        if (bytes == null && files == null) return state
        fun size(value: String, unit: String): Double = (value.toDoubleOrNull() ?: 0.0) * when (unit.lowercase()) {
            "kib", "kbytes" -> 1024.0
            "mib", "mbytes" -> 1048576.0
            "gib", "gbytes" -> 1073741824.0
            "tib", "tbytes" -> 1099511627776.0
            else -> 1.0
        }
        fun duration(value: String): Double? {
            if (value == "-") return null
            val parts = Regex("([0-9.]+)([hms])").findAll(value).toList()
            if (parts.isEmpty()) return null
            return parts.sumOf { it.groupValues[1].toDouble() * when (it.groupValues[2]) { "h" -> 3600; "m" -> 60; else -> 1 } }
        }
        val running = state.status == JobStatus.RUNNING || state.status == JobStatus.PAUSED
        val active = report.substringAfter("Transferring:", "").split('*').drop(1)
        return state.copy(
            bytes = bytes?.let { size(it.groupValues[1], it.groupValues[2]).toLong() } ?: state.bytes,
            totalBytes = bytes?.let { size(it.groupValues[3], it.groupValues[4]).toLong() } ?: state.totalBytes,
            progressPercent = bytes?.groupValues?.get(5)?.toInt()?.coerceIn(0, if (running) 99 else 100) ?: state.progressPercent,
            speedBytesPerSecond = bytes?.let { size(it.groupValues[6], it.groupValues[7]) } ?: state.speedBytesPerSecond,
            etaSeconds = if (bytes != null) duration(bytes.groupValues[8])?.toLong() else state.etaSeconds,
            transfers = files?.groupValues?.get(1)?.toLong() ?: state.transfers,
            totalTransfers = files?.groupValues?.get(2)?.toLong() ?: state.totalTransfers,
            activeTransfers = active.size,
            currentFile = active.firstOrNull()?.substringBeforeLast(':')?.trim()?.takeIf { it.isNotBlank() },
            elapsedSeconds = Regex("Elapsed time:\\s*(\\S+)").find(report)?.groupValues?.get(1)?.let { duration(it) } ?: state.elapsedSeconds
        )
    }

    fun fromStats(state: JobState, stats: JSONObject): JobState {
        val bytes = stats.optLong("bytes", 0).coerceAtLeast(0)
        val totalBytes = stats.optLong("totalBytes", 0).coerceAtLeast(0)
        val transfers = stats.optLong("transfers", 0).coerceAtLeast(0)
        val totalTransfers = stats.optLong("totalTransfers", 0).coerceAtLeast(0)
        val percent = when {
            totalBytes > 0 -> (100.0 * bytes / totalBytes).roundToInt()
            totalTransfers > 0 -> (100.0 * transfers / totalTransfers).roundToInt()
            else -> state.progressPercent
        }
        val running = state.status == JobStatus.RUNNING || state.status == JobStatus.PAUSED
        return state.copy(
            progressPercent = percent.coerceIn(0, if (running) 99 else 100),
            bytes = bytes, totalBytes = totalBytes,
            transfers = transfers, totalTransfers = totalTransfers,
            activeTransfers = stats.optJSONArray("transferring")?.length() ?: 0,
            speedBytesPerSecond = stats.optDouble("speed", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            etaSeconds = if (stats.isNull("eta")) null else stats.optLong("eta").takeIf { it >= 0 },
            elapsedSeconds = stats.optDouble("elapsedTime", 0.0),
            currentFile = stats.optJSONArray("transferring")?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() },
            errors = stats.optInt("errors", 0)
        )
    }

    fun formatSpeed(bytesPerSecond: Double): String = String.format(
        Locale.getDefault(), "%.2f MB/s", if (bytesPerSecond.isFinite()) bytesPerSecond.coerceAtLeast(0.0) / 1_000_000.0 else 0.0
    )
}

/** Counts unique successful file events, never partial reads or failed attempts. */
class TransferCompletionLedger {
    private val completed = mutableMapOf<String, Long?>()

    @Synchronized fun snapshot(): Map<String, Long?> = completed.toMap()

    @Synchronized fun record(log: JSONObject): Pair<Long, Long?>? {
        if (!log.optString("level").equals("info", true)) return null
        val message = log.optString("msg")
        if (!Regex("^(?:Multi-thread )?Copied \\(").containsMatchIn(message) && !message.startsWith("Moved (server-side)")) return null
        val name = log.optString("object").takeIf { it.isNotBlank() } ?: return null
        val size = log.optLong("size", -1).takeIf { it >= 0 }
        completed[name] = size
        return completed.size.toLong() to if (completed.values.any { it == null }) null else completed.values.sumOf { it ?: 0 }
    }
}

/** Terminal redraws suppress periodic JSON stats; the GUI owns stats output. */
object TransferOutput {
    fun normalize(userArgs: List<String>): List<String> {
        val valued = setOf("--stats", "--stats-log-level", "--stats-unit", "--stats-one-line-date-format")
        val toggles = setOf("--progress", "-P", "--use-json-log", "--stats-one-line", "--stats-one-line-date")
        val result = mutableListOf<String>()
        var index = 0
        while (index < userArgs.size) {
            val value = userArgs[index++]
            val flag = value.substringBefore('=')
            if (flag in valued) {
                if ('=' !in value && index < userArgs.size) index++
            } else if (flag !in toggles) result += value
        }
        return result
    }

    val flags = listOf("--use-json-log", "--stats", "1s", "--stats-log-level", "NOTICE")
}
