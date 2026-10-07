package dev.galaxy.rclonecards.service

import android.content.Context
import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object JobRepository {
    private val _jobs = MutableStateFlow<Map<String, JobState>>(emptyMap())
    val jobs: StateFlow<Map<String, JobState>> = _jobs.asStateFlow()

    fun init(@Suppress("UNUSED_PARAMETER") context: Context) = Unit

    fun get(cardId: String): JobState? = _jobs.value[cardId]

    @Synchronized
    fun put(state: JobState) {
        _jobs.value = _jobs.value.toMutableMap().apply { put(state.cardId, state) }
    }

    @Synchronized
    fun update(cardId: String, transform: (JobState) -> JobState) {
        val current = _jobs.value[cardId] ?: return
        _jobs.value = _jobs.value.toMutableMap().apply { put(cardId, transform(current)) }
    }

    @Synchronized
    fun appendLog(cardId: String, line: String) {
        update(cardId) { old ->
            val next = (old.logs + line).takeLast(300)
            old.copy(logs = next)
        }
    }

    @Synchronized
    fun markIdle(cardId: String) {
        update(cardId) { it.copy(status = JobStatus.IDLE) }
    }
}
