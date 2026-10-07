package dev.galaxy.rclonecards.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Küçük, yerel uygulama ayarları. Kimlik bilgisi içermez. */
object AppSettings {
    private const val PREFS = "rclone_cards_settings"
    private const val KEY_TRANSFERS = "default_transfers"
    private const val KEY_CHECKERS = "default_checkers"
    private const val KEY_NOTIFY_COMPLETION = "notify_completion"

    private lateinit var appContext: Context

    private val _defaultTransfers = MutableStateFlow(4)
    val defaultTransfers: StateFlow<Int> = _defaultTransfers.asStateFlow()

    private val _defaultCheckers = MutableStateFlow(8)
    val defaultCheckers: StateFlow<Int> = _defaultCheckers.asStateFlow()

    private val _notifyOnCompletion = MutableStateFlow(true)
    val notifyOnCompletion: StateFlow<Boolean> = _notifyOnCompletion.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        val p = prefs()
        _defaultTransfers.value = p.getInt(KEY_TRANSFERS, 4).coerceIn(1, 64)
        _defaultCheckers.value = p.getInt(KEY_CHECKERS, 8).coerceIn(1, 128)
        _notifyOnCompletion.value = p.getBoolean(KEY_NOTIFY_COMPLETION, true)
    }

    fun setDefaults(transfers: Int, checkers: Int) {
        val t = transfers.coerceIn(1, 64)
        val c = checkers.coerceIn(1, 128)
        _defaultTransfers.value = t
        _defaultCheckers.value = c
        prefs().edit().putInt(KEY_TRANSFERS, t).putInt(KEY_CHECKERS, c).apply()
    }

    fun setNotifyOnCompletion(enabled: Boolean) {
        _notifyOnCompletion.value = enabled
        prefs().edit().putBoolean(KEY_NOTIFY_COMPLETION, enabled).apply()
    }

    fun reset() {
        prefs().edit().clear().apply()
        _defaultTransfers.value = 4
        _defaultCheckers.value = 8
        _notifyOnCompletion.value = true
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
