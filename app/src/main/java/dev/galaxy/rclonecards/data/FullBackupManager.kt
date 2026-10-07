package dev.galaxy.rclonecards.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FullBackupManager {

    fun exportJson(): String {
        val iconData = JSONObject()
        CardStore.cards.value.forEach { card ->
            val path = card.customIconPath
            if (path.isNotBlank()) {
                val file = File(path)
                if (file.isFile) {
                    iconData.put(card.id, Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
                }
            }
        }

        return JSONObject().apply {
            put("format", "RcloneCardsFullBackup")
            put("version", 1)
            put("rcloneConfig", ConfigManager.readText())
            put("cards", JSONArray(CardStore.exportJson()))
            put("settings", JSONObject().apply {
                put("defaultTransfers", AppSettings.defaultTransfers.value)
                put("defaultCheckers", AppSettings.defaultCheckers.value)
                put("notifyOnCompletion", AppSettings.notifyOnCompletion.value)
            })
            put("customIcons", iconData)
        }.toString(2)
    }

    fun importJson(context: Context, raw: String): Result<Int> = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == "RcloneCardsFullBackup") {
            "This is not a Rclone Cards full backup."
        }

        ConfigManager.writeText(root.optString("rcloneConfig", ""))

        val settings = root.optJSONObject("settings") ?: JSONObject()
        AppSettings.setDefaults(
            settings.optInt("defaultTransfers", 4),
            settings.optInt("defaultCheckers", 8)
        )
        AppSettings.setNotifyOnCompletion(settings.optBoolean("notifyOnCompletion", true))

        val cards = root.optJSONArray("cards") ?: JSONArray()
        val icons = root.optJSONObject("customIcons") ?: JSONObject()
        val iconDir = File(context.filesDir, "card-icons").apply { mkdirs() }

        for (i in 0 until cards.length()) {
            val obj = cards.getJSONObject(i)
            val id = obj.optString("id")
            val encoded = if (id.isNotBlank()) icons.optString(id, "") else ""
            if (encoded.isNotBlank()) {
                val out = File(iconDir, "$id.img")
                out.writeBytes(Base64.decode(encoded, Base64.DEFAULT))
                obj.put("customIconPath", out.absolutePath)
            } else {
                obj.put("customIconPath", "")
            }
        }

        CardStore.importJson(cards.toString()).getOrThrow()
    }
}
