package dev.galaxy.rclonecards.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.galaxy.rclonecards.engine.RcloneConfigStep
import dev.galaxy.rclonecards.engine.RcloneConfigWizard
import dev.galaxy.rclonecards.service.DriveSetupService
import kotlinx.coroutines.launch

/** Activity-owned state: rotating or opening the browser must not cancel setup. */
class DriveSetupViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    val open = mutableStateOf(saved.get<Boolean>("driveOpen") ?: false)
    val step = mutableStateOf<RcloneConfigStep?>(null)
    val answer = mutableStateOf("")
    val busy = mutableStateOf(false)
    val remoteName = mutableStateOf(saved.get<String>("driveRemote") ?: "gdrive")
    val error = mutableStateOf<String?>(if (open.value) {
        "Android restarted the app. Start setup again with an unused name, or test a saved connection in Manage Google Drive. Existing connections were preserved."
    } else null)

    fun show() {
        open.value = true
        saved["driveOpen"] = true
    }

    fun start() = request {
        saved["driveRemote"] = remoteName.value
        RcloneConfigWizard.startDrive(getApplication(), remoteName.value)
    }

    fun next() {
        val question = step.value?.question ?: return
        val value = answer.value // Empty is a valid answer, never replace it with a previous value.
        request { RcloneConfigWizard.answerDrive(getApplication(), remoteName.value, question.state, value) }
    }

    fun verify() = request { RcloneConfigWizard.verifyDrive(getApplication(), remoteName.value) }

    private fun request(block: suspend () -> Result<RcloneConfigStep>) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                ContextCompat.startForegroundService(app, Intent(app, DriveSetupService::class.java))
                block().onSuccess { result ->
                    step.value = result
                    answer.value = result.question?.defaultValue.orEmpty()
                    error.value = result.question?.error?.takeIf { it.isNotBlank() }
                        ?: result.verificationMessage.takeIf { result.done && !result.verified }
                    if (result.done) RcloneConfigWizard.finish(app)
                }.onFailure { failure ->
                    error.value = failure.message ?: "Drive setup failed."
                }
            } catch (failure: Exception) {
                error.value = failure.message ?: "Drive setup failed."
            } finally {
                busy.value = false
                app.stopService(Intent(app, DriveSetupService::class.java))
            }
        }
    }

    fun close() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                RcloneConfigWizard.cancel(getApplication())
                open.value = false
                saved["driveOpen"] = false
                step.value = null
                answer.value = ""
                error.value = null
            } finally {
                busy.value = false
            }
        }
    }
}
