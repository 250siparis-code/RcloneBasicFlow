package dev.galaxy.rclonecards.engine

import android.app.Activity
import android.content.Intent
import android.net.Uri
import dev.galaxy.rclonecards.data.ConfigManager
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object DriveConnector {

    fun connect(
        activity: Activity,
        remoteName: String,
        clientId: String,
        clientSecret: String,
        onResult: (Result<String>) -> Unit
    ) {
        thread(name = "rclone-drive-oauth") {
            val result = runCatching {
                require(RcloneEngine.isBinaryAvailable(activity)) { "rclone engine is not available" }

                val name = remoteName.trim().ifBlank { "gdrive" }
                val id = clientId.trim()
                val secret = clientSecret.trim()

                require(id.isNotBlank()) {
                    "Google OAuth client ID is required. Import an existing rclone.conf instead, or enter your own Desktop OAuth client ID."
                }
                require(secret.isNotBlank()) { "Google OAuth client secret is required." }

                val args = listOf(
                    RcloneEngine.binary(activity).absolutePath,
                    "authorize",
                    "drive",
                    "--auth-no-open-browser",
                    "--drive-client-id", id,
                    "--drive-client-secret", secret,
                    "--drive-scope", "drive"
                )

                val process = ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start()

                val output = StringBuilder()
                val browserOpened = AtomicBoolean(false)
                val authUrl = Regex("https?://(?:127\\.0\\.0\\.1|localhost):53682/auth[^\\s]*")

                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        output.appendLine(line)
                        val url = authUrl.find(line)?.value?.trimEnd('.', ',', ';')
                        if (url != null && browserOpened.compareAndSet(false, true)) {
                            activity.runOnUiThread {
                                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    }
                }

                val exit = process.waitFor()
                if (exit != 0) {
                    error(output.toString().trim().ifBlank { "rclone authorize failed with exit code $exit" })
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
                    ?: error("Authorization finished but the OAuth token could not be read.")

                JSONObject(tokenLine)

                ConfigManager.upsertDriveRemote(
                    remoteName = name,
                    clientId = id,
                    clientSecret = secret,
                    tokenJson = tokenLine
                )

                name
            }

            activity.runOnUiThread { onResult(result) }
        }
    }
}
