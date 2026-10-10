package dev.galaxy.rclonecards.engine

import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Closing a child's pipes during cancellation must not crash the app's reader thread. */
object TransferStream {
    fun read(reader: BufferedReader, onLine: (String) -> Unit, onReadError: (IOException) -> Unit) {
        try {
            reader.use { input ->
                while (true) onLine(input.readLine() ?: break)
            }
        } catch (error: IOException) {
            onReadError(error)
        }
    }
}

/** Called off the main thread; interrupt first, then bound the cancellation wait. */
object TransferProcess {
    fun stop(process: Process, interrupt: () -> Unit) {
        if (!process.isAlive) return
        try {
            interrupt()
        } catch (_: Exception) {
            process.destroy()
        }
        if (!process.waitFor(2500, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            process.waitFor(1000, TimeUnit.MILLISECONDS)
        }
    }
}
