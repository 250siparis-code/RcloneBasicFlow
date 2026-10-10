package dev.galaxy.rclonecards.engine

import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TransferProgressTest {
    private val running = JobState("id", "Copy", JobStatus.RUNNING)

    @Test fun suppliedPhoneLogSeparatesActiveFilesFromCompletedFiles() {
        val report = "Transferred: 29.309 MiB / 381.154 MiB, 8%, 2.539 MiB/s, ETA 2m18s Checks: 1 / 1, 100%, Listed 4 Transferred: 0 / 2, 0% Elapsed time: 11.9s Transferring: * Insta.apk: 4% / 167.977 MiB, 744.668 KiB/s, 3m39s * TikTok.apk: 9% / 213.176 MiB, 1.812 MiB/s, 1m45s"
        val state = TransferProgress.fromText(running, report)
        assertEquals(8, state.progressPercent)
        assertEquals(0L, state.transfers)
        assertEquals(2L, state.totalTransfers)
        assertEquals(2, state.activeTransfers)
        assertEquals(2.539 * 1048576, state.speedBytesPerSecond, 0.01)
        assertEquals(138L, state.etaSeconds)
        assertEquals(11.9, state.elapsedSeconds, 0.01)
        assertEquals("Insta.apk", state.currentFile)
    }

    @Test fun fallbackPercentageCanFallWhenTotalGrowsAndNeverConfusesFilePercent() {
        val state = TransferProgress.fromText(running.copy(progressPercent = 80), "Transferred: 2 MiB / 10 MiB, 20%, 1 MiB/s, ETA 8s\nTransferred: 1 / 2, 50%\nElapsed time: 1m2s")
        assertEquals(20, state.progressPercent)
        assertEquals(1L, state.transfers)
        assertEquals(62.0, state.elapsedSeconds, 0.0)
    }

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
