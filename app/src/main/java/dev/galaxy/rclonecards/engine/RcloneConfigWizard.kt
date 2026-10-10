package dev.galaxy.rclonecards.engine

import android.content.Context
import dev.galaxy.rclonecards.BuildConfig
import android.os.Handler
import android.os.Looper
import java.util.UUID
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import android.content.Intent
import android.net.Uri
import dev.galaxy.rclonecards.data.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    val question: RcloneConfigQuestion? = null,
    val verified: Boolean = false,
    val verificationMessage: String = ""
)

object RcloneConfigWizard {
    private var rcAddress = ""
    private val rcPassword = UUID.randomUUID().toString()
    private val sessionConfig: File
        get() = File(ConfigManager.cacheDir, "drive-setup.conf")
    private const val OAUTH_TEMPLATE_ASSET = "oauth-template.html"
    private val serverLock = Any()

    @Volatile
    private var serverProcess: Process? = null

    @Volatile
    private var pendingRemoteName: String? = null

    suspend fun startDrive(
        activity: Context,
        remoteName: String
    ): Result<RcloneConfigStep> = withContext(Dispatchers.IO) {
        runCatching {
            val name = sanitizeRemoteName(remoteName)
            require(!ConfigManager.hasRemote(name)) {
                "Remote '$name' already exists. Choose another name; existing connections are never overwritten."
            }
            stopServer(activity)
            sessionConfig.writeText("")
            pendingRemoteName = name
            ensureServer(activity)

            val payload = JSONObject().apply {
                put("name", name)
                put("type", "drive")
                put("parameters", configParameters(activity))
                put("opt", JSONObject().apply {
                    put("nonInteractive", true)
                    put("all", true)
                    put("obscure", true)
                })
            }

            val step = parseStep(callConfigWithOAuth(activity, "config/create", payload))
            require(!step.done) { "rclone skipped the configuration questions. No connection was saved." }
            step
        }
    }

    suspend fun answerDrive(
        activity: Context,
        remoteName: String,
        state: String,
        result: String
    ): Result<RcloneConfigStep> = withContext(Dispatchers.IO) {
        runCatching {
            val name = sanitizeRemoteName(remoteName)
            require(pendingRemoteName == name && serverProcess?.isAlive == true) {
                "Setup was interrupted. Close this window and start again; existing connections are unchanged."
            }
            require(state.isNotBlank()) { "Missing rclone continuation state." }
            val payload = JSONObject().apply {
                put("name", name)
                put("type", "drive")
                put("parameters", configParameters(activity))
                put("opt", JSONObject().apply {
                    put("nonInteractive", true)
                    put("all", true)
                    put("obscure", true)
                    put("continue", true)
                    put("state", state)
                    put("result", result)
                })
            }

            finalizeIfDone(
                activity = activity,
                remoteName = name,
                step = parseStep(
                    callConfigWithOAuth(
                        activity = activity,
                        endpoint = "config/update",
                        payload = payload
                    )
                )
            )
        }
    }

    suspend fun verifyDrive(
        activity: Context,
        remoteName: String
    ): Result<RcloneConfigStep> = withContext(Dispatchers.IO) {
        runCatching {
            val name = sanitizeRemoteName(remoteName)
            require(ConfigManager.hasRemote(name)) {
                "Remote '$name' was not written to rclone.conf."
            }
            verifyCompletedRemote(activity, name)
        }
    }

    suspend fun cancel(activity: Context) = withContext(Dispatchers.IO) {
        runCatching { rcCall(activity, "config/oauthstop", JSONObject(), 5) }
        // Never delete a persisted remote on cancellation: OAuth may have succeeded
        // even when the UI lost its continuation state.
        pendingRemoteName = null
        stopServer(activity)
        sessionConfig.delete()
    }

    suspend fun finish(activity: Context) = withContext(Dispatchers.IO) {
        pendingRemoteName = null
        stopServer(activity)
        sessionConfig.delete()
    }

    private fun configParameters(activity: Context): JSONObject =
        JSONObject().apply {
            put("config_template_file", ensureOAuthTemplate(activity).absolutePath)
        }

    private fun ensureOAuthTemplate(activity: Context): File {
        val directory = ConfigManager.configFile.parentFile
            ?: error("rclone configuration directory is unavailable.")
        directory.mkdirs()

        val destination = File(directory, OAUTH_TEMPLATE_ASSET)
        activity.assets.open(OAUTH_TEMPLATE_ASSET).use { input ->
            val template = input.bufferedReader().readText()
            destination.writeText(template.replace("basic-rclone-flow://", "${BuildConfig.OAUTH_SCHEME}://"))
        }
        return destination
    }

    private fun finalizeIfDone(
        activity: Context,
        remoteName: String,
        step: RcloneConfigStep
    ): RcloneConfigStep {
        if (!step.done) return step
        ConfigManager.addRemoteFromSetup(remoteName, sessionConfig.readText()).getOrThrow()
        return verifyCompletedRemote(activity, remoteName)
    }

    private fun verifyCompletedRemote(
        activity: Context,
        remoteName: String
    ): RcloneConfigStep {
        val verification = RcloneEngine.testRemote(
            context = activity,
            remoteName = remoteName,
            timeoutSeconds = 45
        )

        return verification.fold(
            onSuccess = {
                RcloneConfigStep(
                    done = true,
                    verified = true,
                    verificationMessage = "Google Drive connection verified."
                )
            },
            onFailure = { error ->
                RcloneConfigStep(
                    done = true,
                    verified = false,
                    verificationMessage = buildString {
                        append("Configuration was saved, but the live Google Drive test failed.")
                        error.message?.takeIf { it.isNotBlank() }?.let {
                            append(" ")
                            append(it)
                        }
                    }
                )
            }
        )
    }

    private fun sanitizeRemoteName(value: String): String {
        val name = value.trim().removeSuffix(":").ifBlank { "gdrive" }
        require(name.none { it == '[' || it == ']' || it == '\n' || it == '\r' || it == ':' }) {
            "Invalid remote name."
        }
        return name
    }

    private fun ensureServer(activity: Context) {
        synchronized(serverLock) {
            val existing = serverProcess
            if (existing != null && existing.isAlive) return

            rcAddress = "127.0.0.1:" + ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress()).use { it.localPort }

            val process = ProcessBuilder(
                RcloneEngine.binary(activity).absolutePath,
                "--config", sessionConfig.absolutePath,
                "--cache-dir", ConfigManager.cacheDir.absolutePath,
                "rcd",
                "--rc-addr", rcAddress,
                "--rc-user", "setup",
                "--rc-pass", rcPassword,
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
            for (attempt in 0 until 50) {
                if (!process.isAlive) {
                    error("rclone RC server stopped before becoming ready.")
                }

                val probe = runCatching {
                    rcCall(activity, "rc/noop", JSONObject(), 2)
                }.getOrNull()

                if (probe != null) {
                    ready = true
                    break
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

    private fun stopServer(activity: Context) {
        val process = serverProcess ?: return

        runCatching {
            rcCall(activity, "core/quit", JSONObject(), 3)
        }

        runCatching {
            process.waitFor(1200, TimeUnit.MILLISECONDS)
        }

        if (process.isAlive) process.destroy()
        if (process.isAlive) process.destroyForcibly()
        serverProcess = null
    }

    private fun callConfigWithOAuth(
        activity: Context,
        endpoint: String,
        payload: JSONObject
    ): JSONObject {
        require(endpoint == "config/create" || endpoint == "config/update") {
            "Unsupported rclone config endpoint: $endpoint"
        }

        val process = startRcProcess(
            activity = activity,
            endpoint = endpoint,
            payload = payload
        )

        val output = StringBuilder()
        val browserOpened = AtomicBoolean(false)
        val browserError = AtomicReference<Exception?>(null)

        val reader = thread(name = "rclone-config-call-output") {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> output.appendLine(line) }
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
                    if (authUrl.isNotBlank() && !browserOpened.get()) {
                        val uri = Uri.parse(authUrl)
                        val host = uri.host.orEmpty()
                        val port = if (uri.port > 0) uri.port else 53682
                        val local = host == "127.0.0.1" || host == "localhost"
                        val ready = local && runCatching {
                            java.net.Socket().use { socket ->
                                socket.connect(java.net.InetSocketAddress(host, port), 700)
                            }
                            true
                        }.getOrDefault(false)
                        if (ready && browserOpened.compareAndSet(false, true)) {
                            val completed = CountDownLatch(1)
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    activity.startActivity(Intent(Intent.ACTION_VIEW, uri)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (error: Exception) {
                                    browserError.set(error)
                                } finally {
                                    completed.countDown()
                                }
                            }
                            completed.await(5, TimeUnit.SECONDS)
                            if (browserError.get() != null) {
                                runCatching { rcCall(activity, "config/oauthstop", JSONObject(), 3) }
                            }
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

        browserError.get()?.let { error("Could not open the browser: ${it.message}") }
        val raw = output.toString().trim()
        if (process.exitValue() != 0) {
            error(parseJsonObject(raw)?.optString("error")?.takeIf { it.isNotBlank() }
                ?: "rclone config request failed (exit ${process.exitValue()}).")
        }

        return parseJsonObject(raw)
            ?: error(raw.ifBlank { "rclone returned an invalid config response." })
    }

    private fun rcCall(
        activity: Context,
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
            error(parseJsonObject(raw)?.optString("error")?.takeIf { it.isNotBlank() }
                ?: "rclone RC call failed: $endpoint (exit ${process.exitValue()}).")
        }

        return parseJsonObject(raw) ?: JSONObject()
    }

    private fun startRcProcess(
        activity: Context,
        endpoint: String,
        payload: JSONObject
    ): Process =
        ProcessBuilder(
            RcloneEngine.binary(activity).absolutePath,
            "rc",
            "--url", "http://$rcAddress",
            "--user", "setup",
            "--pass", rcPassword,
            endpoint,
            "--json", payload.toString()
        )
            .redirectErrorStream(true)
            .start()

    internal fun parseStep(response: JSONObject): RcloneConfigStep {
        val state = response.optString("State", "")

        val responseError = response.optString("Error", "")
        val option = response.optJSONObject("Option")
        if (option == null && response.has("State") && state.isBlank()) {
            require(responseError.isBlank()) { responseError }
            return RcloneConfigStep(done = true)
        }
        require(state.isNotBlank()) { "rclone returned a question without continuation state." }
        val questionOption = option
            ?: error("rclone did not return a configuration question.")

        val examples = questionOption.optJSONArray("Examples")
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

        return RcloneConfigStep(
            done = false,
            question = RcloneConfigQuestion(
                state = state,
                name = questionOption.optString("Name", "rclone option"),
                help = questionOption.optString("Help", ""),
                defaultValue = jsonValueToString(questionOption.opt("Default")),
                defaultString = questionOption.optString("DefaultStr", ""),
                examples = examples,
                required = questionOption.optBoolean("Required", false),
                isPassword = questionOption.optBoolean("IsPassword", false) || questionOption.optString("Name") in setOf("client_secret", "config_token"),
                type = questionOption.optString("Type", "string"),
                exclusive = questionOption.optBoolean("Exclusive", false),
                error = response.optString("Error", "")
            )
        )
    }

    private fun jsonValueToString(value: Any?): String =
        when (value) {
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

    internal fun parseJsonObject(raw: String): JSONObject? {
        if (raw.isBlank()) return null

        runCatching { return JSONObject(raw) }

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
            .mapNotNull { candidate -> runCatching { JSONObject(candidate) }.getOrNull() }
            .firstOrNull()
    }
}
