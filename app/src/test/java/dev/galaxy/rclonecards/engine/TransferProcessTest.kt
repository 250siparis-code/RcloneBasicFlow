package dev.galaxy.rclonecards.engine

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class TransferProcessTest {
    private class Child(private val exitsOnInterrupt: Boolean) : Process() {
        var alive = true
        var interrupted = false
        var forced = false
        override fun isAlive() = alive
        override fun getInputStream() = java.io.ByteArrayInputStream(byteArrayOf())
        override fun getErrorStream() = java.io.ByteArrayInputStream(byteArrayOf())
        override fun getOutputStream() = java.io.ByteArrayOutputStream()
        override fun waitFor() = 130
        override fun waitFor(timeout: Long, unit: TimeUnit) = !alive
        override fun exitValue(): Int { if (alive) throw IllegalThreadStateException(); return 130 }
        override fun destroy() { alive = false }
        override fun destroyForcibly(): Process { forced = true; alive = false; return this }
        fun interrupt() { interrupted = true; if (exitsOnInterrupt) alive = false }
    }

    @Test fun interruptStopsOnlySelectedChildWithoutForce() {
        val selected = Child(true)
        val other = Child(true)
        TransferProcess.stop(selected) { selected.interrupt() }
        assertTrue(selected.interrupted)
        assertFalse(selected.forced)
        assertTrue(other.isAlive)
    }
    @Test fun unresponsiveChildGetsBoundedForceFallback() {
        val selected = Child(false)
        TransferProcess.stop(selected) { selected.interrupt() }
        assertTrue(selected.forced)
        assertFalse(selected.isAlive)
    }
    @Test fun missingPidFallsBackToDestroy() {
        val selected = Child(false)
        TransferProcess.stop(selected) { throw IllegalArgumentException("PID unavailable") }
        assertFalse(selected.isAlive)
        assertFalse(selected.forced)
    }
    @Test fun closedPipeDoesNotEscapeReaderThread() {
        val reader = object : BufferedReader(StringReader("")) {
            override fun readLine(): String? = throw IOException("Stream closed")
        }
        var handled = false
        TransferStream.read(reader, { fail("No line expected") }, { handled = true })
        assertTrue(handled)
    }
}
