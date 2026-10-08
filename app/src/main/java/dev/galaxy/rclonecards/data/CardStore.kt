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
    private const val KEY_LEGACY_SAMPLES_REMOVED = "legacy_sample_cards_removed_v2"
    private val LEGACY_SAMPLE_IDS = setOf("ga34-internal", "camera-backup")

    private lateinit var appContext: Context
    private val _cards = MutableStateFlow<List<TaskCard>>(emptyList())
    val cards: StateFlow<List<TaskCard>> = _cards.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        val prefs = prefs()
        val loaded = if (prefs.contains(KEY_CARDS)) load() else emptyList()
        val migrated = if (!prefs.getBoolean(KEY_LEGACY_SAMPLES_REMOVED, false)) {
            loaded.filterNot { it.id in LEGACY_SAMPLE_IDS }
        } else {
            loaded
        }

        _cards.value = migrated
        persist(migrated)
        prefs.edit().putBoolean(KEY_LEGACY_SAMPLES_REMOVED, true).apply()
    }

    fun get(id: String): TaskCard? = _cards.value.firstOrNull { it.id == id }

    fun addBlank(): TaskCard {
        val card = TaskCard(
            id = UUID.randomUUID().toString(),
            title = "New Task",
            subtitle = "",
            actionLabel = "Run",
            command = "",
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

    fun clearAll() {
        _cards.value = emptyList()
        persist(emptyList())
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

    fun exportJson(): String = serialize(_cards.value).toString(2)

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
                put("customIconPath", card.customIconPath)
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
                    command = o.optString("command", ""),
                    workDir = o.optString("workDir", "/storage/emulated/0/"),
                    color = runCatching { CardColor.valueOf(o.optString("color", "GREEN")) }
                        .getOrDefault(CardColor.GREEN),
                    customColorHex = o.optString("customColorHex", ""),
                    iconAlpha = o.optDouble("iconAlpha", 1.0).toFloat().coerceIn(0.15f, 1f),
                    customIconPath = o.optString("customIconPath", ""),
                    icon = runCatching { CardIcon.valueOf(o.optString("icon", "PHONE")) }
                        .getOrDefault(CardIcon.PHONE),
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
}
