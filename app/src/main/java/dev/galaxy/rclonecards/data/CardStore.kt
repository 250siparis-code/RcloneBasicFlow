package dev.galaxy.rclonecards.data

import android.content.Context
import dev.galaxy.rclonecards.model.CardColor
import dev.galaxy.rclonecards.model.CardIcon
import dev.galaxy.rclonecards.model.TaskCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object CardStore {
    private const val PREFS = "rclone_cards"
    private const val KEY_CARDS = "cards_json"

    private lateinit var appContext: Context
    private val _cards = MutableStateFlow<List<TaskCard>>(emptyList())
    val cards: StateFlow<List<TaskCard>> = _cards.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        val loaded = load()
        _cards.value = if (loaded.isEmpty()) defaultCards() else loaded
        if (loaded.isEmpty()) persist(_cards.value)
    }

    fun get(id: String): TaskCard? = _cards.value.firstOrNull { it.id == id }

    fun addBlank(): TaskCard {
        val card = TaskCard(
            id = UUID.randomUUID().toString(),
            title = "New Task",
            subtitle = "Custom Command",
            actionLabel = "Run",
            command = "rclone copy \"/storage/emulated/0/Download/\" \"gdrive:Yedek/\" --progress",
            workDir = "/storage/emulated/0/",
            color = CardColor.GREEN,
            icon = CardIcon.FOLDER
        )
        save(card)
        return card
    }

    fun save(card: TaskCard) {
        val next = _cards.value.toMutableList()
        val index = next.indexOfFirst { it.id == card.id }
        if (index >= 0) next[index] = card else next += card
        _cards.value = next
        persist(next)
    }

    fun delete(id: String) {
        val next = _cards.value.filterNot { it.id == id }
        _cards.value = next
        persist(next)
    }

    fun duplicate(id: String): TaskCard? {
        val source = get(id) ?: return null
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            title = source.title + " (Copy)",
            scheduleEnabled = false,
            scheduleHour = null,
            scheduleMinute = null
        )
        save(copy)
        return copy
    }

    /** JSON yedeği: kartların tamamını ve zamanlayıcı ayarlarını taşır. */
    fun exportJson(): String = serialize(_cards.value).toString(2)

    /**
     * JSON yedeğini yükler. Kimliği boş/tekrarlı kartlara yeni UUID verir.
     * Başarılı dönüş değeri yüklenen kart sayısıdır.
     */
    fun importJson(raw: String): Result<Int> = runCatching {
        val arr = JSONArray(raw)
        val parsed = parse(arr)
        val seen = mutableSetOf<String>()
        val sanitized = parsed.map { card ->
            var id = card.id.trim()
            if (id.isBlank() || !seen.add(id)) {
                id = UUID.randomUUID().toString()
                seen.add(id)
            }
            card.copy(id = id)
        }
        _cards.value = sanitized
        persist(sanitized)
        sanitized.size
    }

    fun resetDefaults() {
        val defaults = defaultCards()
        _cards.value = defaults
        persist(defaults)
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun persist(cards: List<TaskCard>) {
        prefs().edit().putString(KEY_CARDS, serialize(cards).toString()).apply()
    }

    private fun serialize(cards: List<TaskCard>): JSONArray = JSONArray().apply {
        cards.forEach { card ->
            put(JSONObject().apply {
                put("id", card.id)
                put("title", card.title)
                put("subtitle", card.subtitle)
                put("actionLabel", card.actionLabel)
                put("command", card.command)
                put("workDir", card.workDir)
                put("color", card.color.name)
                put("customColorHex", card.customColorHex)
                put("iconAlpha", card.iconAlpha.toDouble())
                put("icon", card.icon.name)
                put("transfers", card.transfers ?: JSONObject.NULL)
                put("checkers", card.checkers ?: JSONObject.NULL)
                put("bwlimit", card.bwlimit)
                put("scheduleHour", card.scheduleHour ?: JSONObject.NULL)
                put("scheduleMinute", card.scheduleMinute ?: JSONObject.NULL)
                put("scheduleEnabled", card.scheduleEnabled)
            })
        }
    }

    private fun load(): List<TaskCard> {
        val raw = prefs().getString(KEY_CARDS, null) ?: return emptyList()
        return runCatching { parse(JSONArray(raw)) }.getOrElse { emptyList() }
    }

    private fun parse(arr: JSONArray): List<TaskCard> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
                TaskCard(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    title = o.optString("title", "Task"),
                    subtitle = o.optString("subtitle", ""),
                    actionLabel = o.optString("actionLabel", "Run"),
                    command = o.optString("command", "rclone version"),
                    workDir = o.optString("workDir", "/storage/emulated/0/"),
                    color = runCatching { CardColor.valueOf(o.optString("color", "GREEN")) }.getOrDefault(CardColor.GREEN),
                    customColorHex = o.optString("customColorHex", ""),
                    iconAlpha = o.optDouble("iconAlpha", 1.0).toFloat().coerceIn(0.15f, 1f),
                    icon = runCatching { CardIcon.valueOf(o.optString("icon", "PHONE")) }.getOrDefault(CardIcon.PHONE),
                    transfers = if (o.isNull("transfers")) null else o.optInt("transfers").coerceIn(1, 64),
                    checkers = if (o.isNull("checkers")) null else o.optInt("checkers").coerceIn(1, 128),
                    bwlimit = o.optString("bwlimit", ""),
                    scheduleHour = if (o.isNull("scheduleHour")) null else o.optInt("scheduleHour").coerceIn(0, 23),
                    scheduleMinute = if (o.isNull("scheduleMinute")) null else o.optInt("scheduleMinute").coerceIn(0, 59),
                    scheduleEnabled = o.optBoolean("scheduleEnabled", false)
                )
            )
        }
    }

    private fun defaultCards(): List<TaskCard> = listOf(
        TaskCard(
            id = "ga34-internal",
            title = "G.A34",
            subtitle = "Internal Storage",
            actionLabel = "Sync to Drive",
            command = "rclone sync \"/storage/emulated/0/\" \"gdrive:G.A34/Dahili Hafıza/\" --exclude \"/Android/**\" --progress",
            workDir = "/storage/emulated/0/",
            color = CardColor.GREEN,
            icon = CardIcon.PHONE
        ),
        TaskCard(
            id = "camera-backup",
            title = "Camera",
            subtitle = "DCIM / Camera",
            actionLabel = "Back Up to Drive",
            command = "rclone copy \"/storage/emulated/0/DCIM/Camera/\" \"gdrive:G.A34/Dahili Hafıza/DCIM/Camera/\" --progress",
            workDir = "/storage/emulated/0/DCIM/Camera/",
            color = CardColor.PURPLE,
            icon = CardIcon.CAMERA
        )
    )
}
