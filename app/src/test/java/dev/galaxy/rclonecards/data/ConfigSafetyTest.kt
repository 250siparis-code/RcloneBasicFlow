package dev.galaxy.rclonecards.data

import org.junit.Assert.*
import org.junit.Test

class ConfigSafetyTest {
    @Test(expected = IllegalArgumentException::class)
    fun encryptedConfigurationIsNotCorrupted() {
        ConfigManager.mergeNewDriveRemote("RCLONE_ENCRYPT_V0:\nencrypted-content", "new", "[new]\ntype = drive")
    }
    @Test fun preservesExistingConnectionsExactly() {
        val original = "# keep comment\r\n[existing]\r\ntype = drive\r\ntoken = old-token\r\n"
        val merged = ConfigManager.mergeNewDriveRemote(original, "new", "[new]\ntype = drive\ntoken = new-token\n")
        assertTrue(merged.startsWith(original))
        assertTrue(merged.contains("token = new-token"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun existingRemoteIsNeverOverwritten() {
        ConfigManager.mergeNewDriveRemote("[Gdrive]\ntype = drive\ntoken = old", "gdrive", "[gdrive]\ntype = drive\ntoken = new")
    }
    @Test fun onlySelectedSectionIsInstalled() {
        val merged = ConfigManager.mergeNewDriveRemote("", "new", "[other]\ntype = drive\n[new]\ntype = drive\n[unrelated]\ntype = local")
        assertFalse(merged.contains("[other]"))
        assertFalse(merged.contains("[unrelated]"))
    }
    @Test(expected = IllegalStateException::class)
    fun missingSectionCannotReplaceTheConfig() {
        ConfigManager.mergeNewDriveRemote("[existing]\ntype = drive", "missing", "[other]\ntype = drive")
    }
}
