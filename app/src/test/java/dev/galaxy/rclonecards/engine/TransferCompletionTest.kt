package dev.galaxy.rclonecards.engine

import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TransferCompletionTest {
    @Test fun partialStatsDoNotBecomeCompletedUploadBytes() {
        val ledger = TransferCompletionLedger()
        assertNull(ledger.record(JSONObject("""{"level":"notice","stats":{"bytes":800000000,"totalBytes":900000000}}""")))
        assertNull(ledger.record(JSONObject("""{"level":"error","msg":"Failed to copy","object":"large.mp4","size":786000000}""")))
        assertEquals(1L to 14_000_000L, ledger.record(JSONObject("""{"level":"info","msg":"Copied (new)","object":"small.mp4","size":14000000}""")))
    }

    @Test fun repeatedSuccessEventsCountOneDestinationFile() {
        val ledger = TransferCompletionLedger()
        val log = JSONObject("""{"level":"info","msg":"Copied (new)","object":"sample.mp4","size":14}""")
        ledger.record(log)
        assertEquals(1L to 14L, ledger.record(log))
        assertEquals(2L to 28L, ledger.record(JSONObject("""{"level":"info","msg":"Multi-thread Copied (new)","object":"another.mp4","size":14}""")))
    }

    @Test fun missingFileSizeRemainsUnknown() {
        val ledger = TransferCompletionLedger()
        assertEquals(1L to null, ledger.record(JSONObject("""{"level":"info","msg":"Moved (server-side)","object":"sample.mp4"}""")))
    }

    @Test fun statsUpdatesPreserveConfirmedResultsAndDistinguishActiveFiles() {
        val old = JobState("id", "Copy", JobStatus.RUNNING, confirmedFiles=1, confirmedBytes=14_000_000)
        val state = TransferProgress.fromStats(old, JSONObject("""{"bytes":800000000,"totalBytes":900000000,"transfers":1,"totalTransfers":4,"transferring":[{"name":"a"},{"name":"b"},{"name":"c"}]}"""))
        assertEquals(14_000_000L, state.confirmedBytes)
        assertEquals(1L, state.confirmedFiles)
        assertEquals(3, state.activeTransfers)
        assertEquals(800_000_000L, state.bytes)
    }
}
