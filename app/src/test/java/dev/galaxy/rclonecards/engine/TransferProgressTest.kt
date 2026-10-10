package dev.galaxy.rclonecards.engine

import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TransferProgressTest {
    private val running = JobState("id", "Copy", JobStatus.RUNNING)

    @Test fun liveBytesDriveGlobalProgressAndSpeed() {
        val state = TransferProgress.fromStats(running, JSONObject("""{"bytes":2000000,"totalBytes":8000000,"speed":1500000,"eta":4,"elapsedTime":1.5,"transferring":[{"name":"sample","percentage":100}]}"""))
        assertEquals(25, state.progressPercent)
        assertEquals(1500000.0, state.speedBytesPerSecond, 0.0)
        assertEquals(4L, state.etaSeconds)
        assertEquals("sample", state.currentFile)
    }

    @Test fun serverSideOperationsUseFileCountWhenBytesAreZero() {
        val state = TransferProgress.fromStats(running, JSONObject("""{"bytes":0,"totalBytes":0,"transfers":3,"totalTransfers":4,"eta":null}"""))
        assertEquals(75, state.progressPercent)
        assertNull(state.etaSeconds)
    }

    @Test fun growingTotalMayReducePercentage() {
        val state = TransferProgress.fromStats(running.copy(progressPercent=75), JSONObject("""{"bytes":2,"totalBytes":8}"""))
        assertEquals(25, state.progressPercent)
    }

    @Test fun statsCannotDeclareSuccessfulProcessCompletion() {
        val state = TransferProgress.fromStats(running, JSONObject("""{"bytes":4,"totalBytes":4}"""))
        assertEquals(99, state.progressPercent)
        assertEquals(JobStatus.RUNNING, state.status)
    }

    @Test fun emptyAndInvalidSpeedRemainSafe() {
        val state = TransferProgress.fromStats(running, JSONObject("""{"speed":-5,"eta":null}"""))
        assertEquals(0, state.progressPercent)
        assertEquals(0.0, state.speedBytesPerSecond, 0.0)
        assertNull(state.currentFile)
        assertTrue(TransferProgress.formatSpeed(Double.NaN).endsWith(" MB/s"))
    }

    @Test fun speedUsesDecimalMegabytesPerSecond() {
        assertEquals("1.50 MB/s", TransferProgress.formatSpeed(1_500_000.0).replace(',', '.'))
        assertEquals("0.00 MB/s", TransferProgress.formatSpeed(0.0).replace(',', '.'))
    }

    @Test fun terminalFlagsAreRemovedWithoutChangingTransferCommand() {
        val input = listOf("copy", "/source", "gdrive:dest", "-P", "--stats=0", "--stats-one-line", "--stats-unit", "bits", "--bwlimit", "2M")
        assertEquals(listOf("copy", "/source", "gdrive:dest", "--bwlimit", "2M"), TransferOutput.normalize(input))
        assertFalse(TransferOutput.flags.contains("--progress"))
        assertTrue(TransferOutput.flags.contains("--use-json-log"))
    }
}
