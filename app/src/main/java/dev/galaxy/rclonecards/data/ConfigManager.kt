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
        configFile.writeText(text.replace("\r\n", "\n"))
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

    fun hasRemote(name: String): Boolean = sectionBounds(name) != null

    fun remoteNames(): List<String> =
        readText()
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("[") && it.endsWith("]") && it.length > 2 }
            .map { it.substring(1, it.length - 1) }
            .distinct()
            .toList()

    fun driveRemoteNames(): List<String> =
        remoteNames().filter { remoteType(it).equals("drive", ignoreCase = true) }

    fun remoteType(name: String): String =
        readRemoteBlock(name)
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("type", ignoreCase = true) && it.contains("=") }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()

    fun readRemoteBlock(name: String): String? {
        val lines = readText().lines()
        val bounds = sectionBounds(lines, name) ?: return null
        return lines.subList(bounds.first, bounds.second).joinToString("\n").trimEnd()
    }

    fun replaceRemoteBlock(name: String, block: String): Result<Unit> = runCatching {
        val wanted = sanitizeRemoteName(name)
        val normalized = block.replace("\r\n", "\n").trim()
        require(normalized.isNotBlank()) { "Remote configuration cannot be empty." }

        val blockLines = normalized.lines()
        require(blockLines.firstOrNull()?.trim()?.equals("[$wanted]", ignoreCase = true) == true) {
            "The first line must remain [$wanted]."
        }
        require(
            blockLines.any {
                it.trim().startsWith("type", ignoreCase = true) &&
                    it.substringAfter("=", "").trim().equals("drive", ignoreCase = true)
            }
        ) {
            "Basic Rclone Flow currently supports Google Drive remotes only (type = drive)."
        }

        val lines = readText().lines().toMutableList()
        val bounds = sectionBounds(lines, wanted) ?: error("Remote '$wanted' was not found.")
        val next = buildList {
            addAll(lines.subList(0, bounds.first))
            addAll(blockLines)
            addAll(lines.subList(bounds.second, lines.size))
        }
        writeText(next.joinToString("\n").trimEnd() + "\n")
    }

    fun deleteRemote(name: String): Result<Unit> = runCatching {
        val wanted = sanitizeRemoteName(name)
        val lines = readText().lines().toMutableList()
        val bounds = sectionBounds(lines, wanted) ?: error("Remote '$wanted' was not found.")
        val next = buildList {
            addAll(lines.subList(0, bounds.first))
            addAll(lines.subList(bounds.second, lines.size))
        }
        val cleaned = next.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        writeText(if (cleaned.isBlank()) "" else "$cleaned\n")
    }

    private fun sectionBounds(name: String): Pair<Int, Int>? =
        sectionBounds(readText().lines(), name)

    private fun sectionBounds(lines: List<String>, name: String): Pair<Int, Int>? {
        val wanted = sanitizeRemoteName(name)
        val start = lines.indexOfFirst { line ->
            val trimmed = line.trim()
            trimmed.startsWith("[") &&
                trimmed.endsWith("]") &&
                trimmed.substring(1, trimmed.length - 1).equals(wanted, ignoreCase = true)
        }
        if (start < 0) return null

        var end = lines.size
        for (index in start + 1 until lines.size) {
            val trimmed = lines[index].trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                end = index
                break
            }
        }
        return start to end
    }

    private fun sanitizeRemoteName(value: String): String {
        val name = value.trim().removeSuffix(":")
        require(name.isNotBlank()) { "Remote name is empty." }
        require(name.none { it == '[' || it == ']' || it == '\n' || it == '\r' || it == ':' }) {
            "Invalid remote name."
        }
        return name
    }
}
