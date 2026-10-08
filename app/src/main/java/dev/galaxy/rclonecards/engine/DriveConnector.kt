package dev.galaxy.rclonecards.engine

import android.app.Activity
import android.content.Intent
import android.net.Uri
import dev.galaxy.rclonecards.data.ConfigManager
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object DriveConnector {

    fun connect(
        activity: Activity,
        remoteName: String,
        clientId: String,
        clientSecret: String,
        scope: String,
        rootFolderId: String,
        serviceAccountFile: String,
        teamDrive: String,
        onResult: (Result<String>) -> Unit
    ) {
        thread(name = "rclone-drive-setup") {
            val result = runCatching {
                require(RcloneEngine.isBinaryAvailable(activity)) {
                    "rclone engine is not available"
                }

                val name = remoteName.trim().ifBlank { "gdrive" }
                val id = clientId.trim()
                val secret = clientSecret.trim()
                val selectedScope = scope.trim().ifBlank { "drive" }
                val root = rootFolderId.trim()
                val serviceFile = serviceAccountFile.trim()
                val sharedDrive = teamDrive.trim()

                if (serviceFile.isNotBlank()) {
                    val file = File(serviceFile)
                    require(file.isFile) {
                        "Service Account JSON file was not found."
                    }

                    ConfigManager.upsertDriveRemote(
                        remoteName = name,
                        clientId = id,
                        clientSecret = secret,
                        scope = selectedScope,
                        rootFolderId = root,
                        serviceAccountFile = file.absolutePath,
                        teamDrive = sharedDrive,
                        tokenJson = ""
                    )

                    return@runCatching name
                }

                val args = mutableListOf(
                    RcloneEngine.binary(activity).absolutePath,
                    "authorize",
                    "drive",
                    "--auth-no-open-browser",
                    "--drive-scope",
                    selectedScope
                )

                if (id.isNotBlank()) {
                    args += listOf("--drive-client-id", id)
                }

                if (secret.isNotBlank()) {
                    args += listOf("--drive-client-secret", secret)
                }

                val process = ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start()

                val output = StringBuilder()
                val browserOpened = AtomicBoolean(false)
                val authUrl = Regex(
                    "https?://(?:127\\.0\\.0\\.1|localhost):53682/auth[^\\s]*"
                )

                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        output.appendLine(line)

                        val url = authUrl
                            .find(line)
                            ?.value
                            ?.trimEnd('.', ',', ';')

                        if (url != null && browserOpened.compareAndSet(false, true)) {
                            activity.runOnUiThread {
                                activity.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            }
                        }
                    }
                }

                val exit = process.waitFor()

                if (exit != 0) {
                    error(
                        output.toString()
                            .trim()
                            .ifBlank { "rclone authorize failed with exit code $exit" }
                    )
                }

                val tokenLine = output.lineSequence()
                    .map { it.trim() }
                    .lastOrNull {
                        it.startsWith("{") &&
                            it.endsWith("}") &&
                            it.contains("\"access_token\"")
                    }
                    ?: Regex("\\{[^\\n]*\"access_token\"[^\\n]*\\}")
                        .find(output.toString())
                        ?.value
                    ?: error(
                        "Authorization finished but the OAuth token could not be read."
                    )

                JSONObject(tokenLine)

                ConfigManager.upsertDriveRemote(
                    remoteName = name,
                    clientId = id,
                    clientSecret = secret,
                    scope = selectedScope,
                    rootFolderId = root,
                    serviceAccountFile = "",
                    teamDrive = sharedDrive,
                    tokenJson = tokenLine
                )

                name
            }

            activity.runOnUiThread {
                onResult(result)
            }
        }
    }
}
