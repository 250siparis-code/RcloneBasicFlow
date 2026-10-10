package dev.galaxy.rclonecards.engine

import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class TransferVerificationTest {
    @Test fun destinationPreservesQuotedNamesAndOnlySupportsUnambiguousCommands() {
        assertEquals("gdrive:G.A34/CAMDrive", TransferVerification.destination("rclone copy ~/storage/shared/DCIM/Camera \"gdrive:G.A34/CAMDrive\""))
        assertNull(TransferVerification.destination("rclone copy /source gdrive:dest --config /custom"))
        assertNull(TransferVerification.destination("rclone move /source gdrive:dest"))
    }
    @Test fun missingSecondFilePreventsVerifiedSuccess() {
        assertEquals("Destination file missing: second.apk", TransferVerification.validate(
            linkedMapOf("first.apk" to 14L, "second.apk" to 800L), JSONArray("""[{"Path":"first.apk","Size":14}]""")))
    }
    @Test fun wrongSizePreventsVerifiedSuccess() {
        assertEquals("Destination size mismatch: second.apk", TransferVerification.validate(
            mapOf("second.apk" to 800L), JSONArray("""[{"Path":"second.apk","Size":14}]""")))
    }
    @Test fun exactDestinationFilesAndSizesPass() {
        assertNull(TransferVerification.validate(mapOf("one" to 14L, "two" to 800L),
            JSONArray("""[{"Path":"one","Size":14},{"Path":"two","Size":800}]""")))
    }
    @Test fun duplicateNamesCannotProduceFalseConfirmation() {
        assertNotNull(TransferVerification.validate(mapOf("one" to 14L),
            JSONArray("""[{"Path":"one","Size":14},{"Path":"one","Size":14}]""")))
    }
    @Test fun allBytesSentIsStillAwaitingConfirmationWhileFileIsActive() {
        val state = JobState("id", "Copy", JobStatus.RUNNING, bytes = 47535, totalBytes = 47535, activeTransfers = 1)
        assertTrue(TransferProgress.awaitingConfirmation(state))
        assertFalse(TransferProgress.awaitingConfirmation(state.copy(status = JobStatus.COMPLETED, activeTransfers = 0)))
    }
}
