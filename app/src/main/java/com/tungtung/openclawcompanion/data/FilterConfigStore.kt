package com.tungtung.openclawcompanion.data

import android.content.Context
import android.content.SharedPreferences
import com.tungtung.openclawcompanion.OpenClawCompanionApp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object FilterConfigStore {
    private const val PREFS_NAME = "openclaw_prefs"
    private const val KEY_FILTER = "filters"
    private const val KEY_GATEWAY = "gateway"
    private const val KEY_SMS_HISTORY = "sms_history"
    private const val MAX_SMS_HISTORY = 100

    private val prefs: SharedPreferences by lazy {
        OpenClawCompanionApp.context().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): FilterConfig {
        val raw = prefs.getString(KEY_FILTER, null) ?: return FilterConfig.Default
        return runCatching { json.decodeFromString<FilterConfig>(raw) }
            .getOrElse { FilterConfig.Default }
    }

    fun save(config: FilterConfig) {
        prefs.edit().putString(KEY_FILTER, json.encodeToString(config)).apply()
    }

    fun loadGatewayPrefs(): GatewayPrefs {
        val raw = prefs.getString(KEY_GATEWAY, null) ?: return GatewayPrefs()
        return runCatching { json.decodeFromString<GatewayPrefs>(raw) }
            .getOrElse { GatewayPrefs() }
    }

    fun saveGatewayPrefs(prefsValue: GatewayPrefs) {
        prefs.edit().putString(KEY_GATEWAY, json.encodeToString(prefsValue)).apply()
    }

    fun loadSmsHistory(): List<SmsHistoryEntry> {
        val raw = prefs.getString(KEY_SMS_HISTORY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SmsHistoryEntry>>(raw) }
            .getOrElse { emptyList() }
    }

    fun addSmsHistory(entry: SmsHistoryEntry) {
        val current = loadSmsHistory()
        val updated = (listOf(entry) + current).take(MAX_SMS_HISTORY)
        prefs.edit().putString(KEY_SMS_HISTORY, json.encodeToString(updated)).apply()
    }

    fun clearSmsHistory() {
        prefs.edit().remove(KEY_SMS_HISTORY).apply()
    }
}
