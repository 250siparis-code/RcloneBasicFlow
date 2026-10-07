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
            ?: error("Dosya okunamadı")
        writeText(text)
    }

    fun exportToUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        val bytes = readText().toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("Dosya yazılamadı")
    }
}
