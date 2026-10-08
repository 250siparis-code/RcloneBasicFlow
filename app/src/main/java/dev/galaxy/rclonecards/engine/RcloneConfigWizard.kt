package dev.galaxy.rclonecards.engine

import android.app.Activity
import android.content.Intent
import android.net.Uri
import dev.galaxy.rclonecards.data.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

data class RcloneConfigExample(
    val value: String,
    val help: String
)

data class RcloneConfigQuestion(
    val state: String,
    val name: String,
    val help: String,
    val defaultValue: String,
    val defaultString: String,
    val examples: List<RcloneConfigExample>,
    val required: Boolean,
    val isPassword: Boolean,
    val type: String,
    val exclusive: Boolean,
    val error: String
)

data class RcloneConfigStep(
    val done: Boolean,
    val question: RcloneConfigQuestion? = null
)

object RcloneConfigWizard {
    private const val RC_ADDR = "127.0.0.1:5572"
    private const val RC_URL = "http://127.0.0.1:5572"
    private val serverLock = Any()

    @Volatile
    private var serverProcess: Process? = null

    suspend fun startDrive(
        activity: Activity,
        remoteName: String
    ): Result<RcloneConfigStep> = withContext(Dispatchers.IO) {
        runCatching {
            ensureServer(activity)

            val name = sanitizeRemoteName(remoteName)
            val payload = JSONObject().apply {
                put("name", name)
                put("type", "drive")
                put("parameters", JSONObject())
                put("opt", JSONObject().apply {
                    put("nonInteractive", true)
                    put("all", true)
                    put("obscure", true)
                })
            }

            parseStep(callConfigWithOAuth(activity, payload))
        }
    }

    suspend fun answerDrive(
        activity: Activity,
        remoteName: String,
        state: String,
        result: String
    ): Result<RcloneConfigStep> = withContext(Dispatchers.IO) {
        runCatching {
            ensureServer(activity)

            val name = sanitizeRemoteName(remoteName)
            val payload = JSONObject().apply {
                put("name", name)
                put("type", "drive")
                put("parameters", JSONObject())
                put("opt", JSONObject().apply {
                    put("nonInteractive", true)
                    put("all", true)
                    put("obscure", true)
                    put("continue", true)
                    put("state", state)
                    put("result", result)
                })
            }

            parseStep(callConfigWithOAuth(activity, payload))
        }
    }

    suspend fun cancel(activity: Activity) = withContext(Dispatchers.IO) {
        runCatching { rcCall(activity, "config/oauthstop", JSONObject(), 5) }
        stopServer(activity)
    }

    suspend fun finish(activity: Activity) = withContext(Dispatchers.IO) {
        stopServer(activity)
    }

    private fun sanitizeRemoteName(value: String): String {
        val name = value.trim().removeSuffix(":").ifBlank { "gdrive" }
        require(name.none { it == '[' || it == ']' || it == '\n' || it == '\r' || it == ':' }) {
            "Invalid remote name."
        }
        return name
    }

    private fun ensureServer(activity: Activity) {
        synchronized(serverLock) {
            val existing = serverProcess
            if (existing != null && existing.isAlive) return

            ConfigManager.ensureConfigExists()

            val process = ProcessBuilder(
                RcloneEngine.binary(activity).absolutePath,
                "--config", ConfigManager.configFile.absolutePath,
                "--cache-dir", ConfigManager.cacheDir.absolutePath,
                "rcd",
                "--rc-addr", RC_ADDR,
                "--rc-no-auth",
                "--log-level", "ERROR"
            )
                .redirectErrorStream(true)
                .start()

            serverProcess = process

            thread(name = "rclone-rcd-log", isDaemon = true) {
                runCatching {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { _ -> }
                    }
                }
            }

            var ready = false
            repeat(50) {
                if (!process.isAlive) {
                    error("rclone RC server stopped before becoming ready.")
                }

                val probe = runCatching {
                    rcCall(activity, "rc/noop", JSONObject(), 2)
                }.getOrNull()

                if (probe != null) {
                    ready = true
                    return@repeat
                }

                Thread.sleep(100)
            }

            if (!ready) {
                process.destroyForcibly()
                serverProcess = null
                error("rclone RC server did not start.")
            }
        }
    }

    private fun stopServer(activity: Activity) {
        val process = serverProcess ?: return

        runCatching {
            rcCall(activity, "core/quit", JSONObject(), 3)
        }

        runCatching {
            process.waitFor(1200, TimeUnit.MILLISECONDS)
        }

        if (process.isAlive) {
            process.destroy()
        }
        if (process.isAlive) {
            process.destroyForcibly()
        }

        serverProcess = null
    }

    private fun callConfigWithOAuth(
        activity: Activity,
        payload: JSONObject
    ): JSONObject {
        val process = startRcProcess(
            activity = activity,
            endpoint = "config/create",
            payload = payload
        )

        val output = StringBuilder()
        val browserOpened = AtomicBoolean(false)

        val reader = thread(name = "rclone-config-call-output") {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    output.appendLine(line)
                }
            }
        }

        val oauthPoller = thread(name = "rclone-oauth-poller") {
            val started = System.currentTimeMillis()

            while (process.isAlive && System.currentTimeMillis() - started < 300_000L) {
                runCatching {
                    val status = rcCall(
                        activity = activity,
                        endpoint = "config/oauthstatus",
                        payload = JSONObject(),
                        timeoutSeconds = 3
                    )

                    val authUrl = status.optString("authUrl", "")
                    if (
                        authUrl.isNotBlank() &&
                        browserOpened.compareAndSet(false, true)
                    ) {
                        activity.runOnUiThread {
                            activity.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                            )
                        }
                    }
                }

                Thread.sleep(250)
            }
        }

        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            runCatching {
                rcCall(activity, "config/oauthstop", JSONObject(), 3)
            }
            process.destroyForcibly()
            reader.join(1500)
            oauthPoller.join(1500)
            error("rclone configuration timed out.")
        }

        reader.join(2000)
        oauthPoller.join(2000)

        val raw = output.toString().trim()
        if (process.exitValue() != 0) {
            error(raw.ifBlank { "rclone config request failed." })
        }

        return parseJsonObject(raw)
            ?: error(raw.ifBlank { "rclone returned an invalid config response." })
    }

    private fun rcCall(
        activity: Activity,
        endpoint: String,
        payload: JSONObject,
        timeoutSeconds: Long
    ): JSONObject {
        val process = startRcProcess(activity, endpoint, payload)
        val output = StringBuilder()

        val reader = thread(name = "rclone-rc-output") {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> output.appendLine(line) }
            }
        }

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            reader.join(500)
            error("rclone RC call timed out: $endpoint")
        }

        reader.join(1000)

        val raw = output.toString().trim()
        if (process.exitValue() != 0) {
            error(raw.ifBlank { "rclone RC call failed: $endpoint" })
        }

        return parseJsonObject(raw) ?: JSONObject()
    }

    private fun startRcProcess(
        activity: Activity,
        endpoint: String,
        payload: JSONObject
    ): Process {
        return ProcessBuilder(
            RcloneEngine.binary(activity).absolutePath,
            "rc",
            "--url", RC_URL,
            endpoint,
            "--json", payload.toString()
        )
            .redirectErrorStream(true)
            .start()
    }

    private fun parseStep(response: JSONObject): RcloneConfigStep {
        val state = response.optString("State", "")

        if (response.has("State") && state.isBlank()) {
            return RcloneConfigStep(done = true)
        }

        val option = response.optJSONObject("Option")
            ?: error("rclone did not return a configuration question.")

        val examples = option.optJSONArray("Examples")
            ?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        add(
                            RcloneConfigExample(
                                value = jsonValueToString(item.opt("Value")),
                                help = item.optString("Help", "")
                            )
                        )
                    }
                }
            }
            .orEmpty()

        val question = RcloneConfigQuestion(
            state = state,
            name = option.optString("Name", "rclone option"),
            help = option.optString("Help", ""),
            defaultValue = jsonValueToString(option.opt("Default")),
            defaultString = option.optString("DefaultStr", ""),
            examples = examples,
            required = option.optBoolean("Required", false),
            isPassword = option.optBoolean("IsPassword", false),
            type = option.optString("Type", "string"),
            exclusive = option.optBoolean("Exclusive", false),
            error = response.optString("Error", "")
        )

        return RcloneConfigStep(
            done = false,
            question = question
        )
    }

    private fun jsonValueToString(value: Any?): String {
        return when (value) {
            null, JSONObject.NULL -> ""
            is Boolean -> value.toString()
            is Number -> value.toString()
            is String -> value
            is JSONArray -> buildList {
                for (i in 0 until value.length()) {
                    add(jsonValueToString(value.opt(i)))
                }
            }.joinToString(",")
            else -> value.toString()
        }
    }

    private fun parseJsonObject(raw: String): JSONObject? {
        if (raw.isBlank()) return null

        runCatching {
            return JSONObject(raw)
        }

        val objects = mutableListOf<String>()
        var start = -1
        var depth = 0
        var inString = false
        var escaped = false

        raw.forEachIndexed { index, ch ->
            if (start < 0) {
                if (ch == '{') {
                    start = index
                    depth = 1
                    inString = false
                    escaped = false
                }
                return@forEachIndexed
            }

            if (inString) {
                if (escaped) {
                    escaped = false
                } else {
                    when (ch) {
                        '\\' -> escaped = true
                        '"' -> inString = false
                    }
                }
                return@forEachIndexed
            }

            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        objects += raw.substring(start, index + 1)
                        start = -1
                    }
                }
            }
        }

        return objects
            .asReversed()
            .asSequence()
            .mapNotNull { candidate ->
                runCatching { JSONObject(candidate) }.getOrNull()
            }
            .firstOrNull()
    }
}
