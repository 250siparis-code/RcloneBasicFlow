package dev.galaxy.rclonecards.engine

import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

/** Global transfer statistics, never an individual file's percentage. */
object TransferProgress {
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
