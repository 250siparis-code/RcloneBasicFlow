package dev.galaxy.rclonecards.data

import android.content.Context
import android.net.Uri
import java.io.File

object ConfigManager {
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        ensureConfigExists()
    }

    val configFile: File
        get() = File(appContext.filesDir, "rclone/rclone.conf")

    val cacheDir: File
        get() = File(appContext.cacheDir, "rclone").also { it.mkdirs() }

    fun ensureConfigExists() {
        val file = configFile
        file.parentFile?.mkdirs()
        if (!file.exists()) file.writeText("")
    }

    fun readText(): String {
        ensureConfigExists()
        return configFile.readText()
    }

    fun writeText(text: String) {
        ensureConfigExists()
        configFile.writeText(text)
    }

    fun importFromUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("File could not be read")
        writeText(text)
    }

    fun exportToUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        val bytes = readText().toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("File could not be written")
    }
    fun upsertDriveRemote(
        remoteName: String,
        clientId: String,
        clientSecret: String,
        tokenJson: String
    ) {
        val name = remoteName.trim().ifBlank { "gdrive" }
        require(!name.contains('[') && !name.contains(']') && !name.contains('\n') && !name.contains('\r')) {
            "Invalid remote name"
        }

        val header = "[$name]"
        val kept = mutableListOf<String>()
        var skipping = false

        readText().lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (trimmed == header) {
                    skipping = true
                    return@forEach
                }
                skipping = false
            }
            if (!skipping) kept += line
        }

        while (kept.isNotEmpty() && kept.last().isBlank()) kept.removeAt(kept.lastIndex)

        val block = buildString {
            appendLine(header)
            appendLine("type = drive")
            if (clientId.isNotBlank()) appendLine("client_id = ${clientId.trim()}")
            if (clientSecret.isNotBlank()) appendLine("client_secret = ${clientSecret.trim()}")
            appendLine("scope = drive")
            appendLine("token = ${tokenJson.trim()}")
        }.trimEnd()

        val next = buildString {
            if (kept.isNotEmpty()) {
                append(kept.joinToString("\n").trimEnd())
                append("\n\n")
            }
            append(block)
            append('\n')
        }
        writeText(next)
    }

}
