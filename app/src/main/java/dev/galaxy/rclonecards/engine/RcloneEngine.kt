package dev.galaxy.rclonecards.engine

import android.content.Context
import dev.galaxy.rclonecards.data.AppSettings
import dev.galaxy.rclonecards.data.ConfigManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object RcloneEngine {

    fun binary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, "librclone.so")

    fun isBinaryAvailable(context: Context): Boolean = binary(context).exists()

    fun buildProcessArgs(
        context: Context,
        rawCommand: String,
        transfersOverride: Int? = null,
        checkersOverride: Int? = null,
        bwlimitOverride: String = ""
    ): List<String> {
        val parsed = ShellWords.parse(rawCommand.trim())
        require(parsed.isNotEmpty()) { "Command is empty" }

        val userArgs = parsed.toMutableList()
        if (userArgs.firstOrNull()?.substringAfterLast('/')?.equals("rclone", ignoreCase = true) == true) {
            userArgs.removeAt(0)
        }

        for (i in userArgs.indices) {
            userArgs[i] = normalizeTermuxPath(userArgs[i])
        }

        require(userArgs.isNotEmpty()) { "rclone subcommand is missing" }

        // Normalize user progress flags; the app adds one canonical --progress flag.
        userArgs.removeAll { it == "--progress" || it == "-P" || it.startsWith("--progress=") }

        val args = mutableListOf<String>()
        args += binary(context).absolutePath

        if (!hasOption(userArgs, "--config")) {
            args += listOf("--config", ConfigManager.configFile.absolutePath)
        }
        if (!hasOption(userArgs, "--cache-dir")) {
            args += listOf("--cache-dir", ConfigManager.cacheDir.absolutePath)
        }
        if (!hasOption(userArgs, "--use-json-log")) args += "--use-json-log"
        if (!hasOption(userArgs, "--stats")) args += listOf("--stats", "1s")
        if (!hasOption(userArgs, "--stats-log-level")) args += listOf("--stats-log-level", "NOTICE")
        if (!hasOption(userArgs, "--stats-one-line")) args += "--stats-one-line"
        if (!hasOption(userArgs, "--log-level") && !hasVerboseFlag(userArgs)) {
            args += listOf("--log-level", "INFO")
        }
        args += "--progress"

        val transfers = transfersOverride ?: AppSettings.defaultTransfers.value
        val checkers = checkersOverride ?: AppSettings.defaultCheckers.value
        if (!hasOption(userArgs, "--transfers")) args += listOf("--transfers", transfers.toString())
        if (!hasOption(userArgs, "--checkers")) args += listOf("--checkers", checkers.toString())
        if (bwlimitOverride.isNotBlank() && !hasOption(userArgs, "--bwlimit")) {
            args += listOf("--bwlimit", bwlimitOverride.trim())
        }

        args += userArgs
        return args
    }

    fun referencedRemotes(rawCommand: String): Set<String> {
        val parsed = runCatching { ShellWords.parse(rawCommand.trim()) }.getOrElse { return emptySet() }

        return parsed.mapNotNull { token ->
            val value = token.trim()
            if (value.contains("://")) return@mapNotNull null

            val match = Regex("^([A-Za-z0-9._-]+):(.*)$").matchEntire(value)
                ?: return@mapNotNull null

            val name = match.groupValues[1]
            if (name.length == 1 && name[0].isLetter()) null else name
        }.toSet()
    }

    fun runQuick(context: Context, argsAfterRclone: List<String>, timeoutSeconds: Long = 20): Result<String> = runCatching {
        val args = mutableListOf<String>()
        args += binary(context).absolutePath
        args += listOf("--config", ConfigManager.configFile.absolutePath)
        args += listOf("--cache-dir", ConfigManager.cacheDir.absolutePath)
        args += argsAfterRclone

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        val readerThread = kotlin.concurrent.thread(name = "rclone-quick-output") {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> output.appendLine(line) }
            }
        }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            readerThread.join(1000)
            error("rclone timed out")
        }
        readerThread.join(1000)
        val text = output.toString().trim()
        if (process.exitValue() != 0) error(text.ifBlank { "rclone exit code: ${process.exitValue()}" })
        text
    }

    fun version(context: Context): String = runQuick(context, listOf("version"), 10)
        .getOrElse { "Rclone not found" }
        .lineSequence().firstOrNull().orEmpty()

    fun testRemote(
        context: Context,
        remoteName: String,
        timeoutSeconds: Long = 45
    ): Result<String> = runCatching {
        val name = remoteName.trim().removeSuffix(":")
        require(Regex("^[A-Za-z0-9._-]+$").matches(name)) { "Invalid remote name." }
        require(ConfigManager.hasRemote(name)) { "Remote '$name' is not configured." }
        require(ConfigManager.remoteType(name).equals("drive", ignoreCase = true)) {
            "Only Google Drive remotes are supported in this version."
        }

        runQuick(
            context = context,
            argsAfterRclone = listOf("lsd", "$name:"),
            timeoutSeconds = timeoutSeconds
        ).getOrThrow()

        "Google Drive connection verified."
    }

    fun listRemotes(context: Context): List<String> {
        val raw = runQuick(context, listOf("listremotes", "--long", "--json"), 20).getOrNull().orEmpty().trim()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val arr = when {
                raw.startsWith("[") -> JSONArray(raw)
                raw.startsWith("{") -> JSONObject(raw).optJSONArray("remotes") ?: JSONArray()
                else -> return@runCatching raw.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
            }
            buildList {
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i)
                    if (r != null) {
                        val name = r.optString("name").removeSuffix(":")
                        val type = r.optString("type")
                        val description = r.optString("description")
                        if (name.isNotBlank()) {
                            add(buildString {
                                append(name)
                                append(":")
                                if (type.isNotBlank()) append("  ·  ").append(type)
                                if (description.isNotBlank()) append("  ·  ").append(description)
                            })
                        }
                    } else {
                        val name = arr.optString(i)
                        if (name.isNotBlank()) add(if (name.endsWith(":")) name else "$name:")
                    }
                }
            }
        }.getOrElse {
            raw.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        }
    }

    private fun normalizeTermuxPath(value: String): String {
        val prefixes = listOf(
            "~/storage/shared",
            "/data/data/com.termux/files/home/storage/shared"
        )
        for (prefix in prefixes) {
            if (value == prefix) return "/storage/emulated/0"
            if (value.startsWith("$prefix/")) {
                return "/storage/emulated/0/" + value.removePrefix("$prefix/")
            }
        }
        return value
    }

    private fun hasVerboseFlag(args: List<String>) = args.any {
        it == "-v" || it == "-vv" || it == "-vvv" || it.startsWith("--verbose")
    }

    private fun hasOption(args: List<String>, option: String): Boolean = args.anyIndexed { index, item ->
        item == option || item.startsWith("$option=") || (index > 0 && args[index - 1] == option)
    }

    private inline fun <T> Iterable<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        var i = 0
        for (e in this) {
            if (predicate(i++, e)) return true
        }
        return false
    }
}
