package dev.galaxy.rclonecards.model

enum class CardColor { GREEN, PURPLE, AMBER, BLUE, RED, SLATE }
enum class CardIcon { PHONE, CAMERA, FOLDER, CLOUD, DESKTOP, SETTINGS }
enum class JobStatus { IDLE, QUEUED, RUNNING, PAUSED, COMPLETED, ERROR, STOPPED }

data class TaskCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val command: String,
    val workDir: String = "/storage/emulated/0/",
    val color: CardColor = CardColor.GREEN,
    val icon: CardIcon = CardIcon.PHONE,
    val transfers: Int? = null,
    val checkers: Int? = null,
    val bwlimit: String = "",
    val scheduleHour: Int? = null,
    val scheduleMinute: Int? = null,
    val scheduleEnabled: Boolean = false
)

data class JobState(
    val cardId: String,
    val title: String,
    val status: JobStatus = JobStatus.IDLE,
    val progressPercent: Int = 0,
    val bytes: Long = 0L,
    val totalBytes: Long = 0L,
    val transfers: Long = 0L,
    val totalTransfers: Long = 0L,
    val speedBytesPerSecond: Double = 0.0,
    val etaSeconds: Long? = null,
    val elapsedSeconds: Double = 0.0,
    val currentFile: String? = null,
    val errors: Int = 0,
    val lastError: String? = null,
    val logs: List<String> = emptyList(),
    val exitCode: Int? = null,
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null
)
