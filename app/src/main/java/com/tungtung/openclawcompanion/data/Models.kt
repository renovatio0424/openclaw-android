package com.tungtung.openclawcompanion.data

import kotlinx.serialization.Serializable

@Serializable
data class ForwardedEvent(
    val timestamp: Long,
    val type: String,
    val source: String,
    val text: String
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Serializable
data class FilterConfig(
    val appWhitelist: List<String> = emptyList(),
    val smsIncludeKeywords: List<String> = emptyList(),
    val blockedKeywords: List<String> = emptyList()
) {
    companion object {
        val Default = FilterConfig()
    }
}

@Serializable
data class SmsHistoryEntry(
    val timestamp: Long,
    val sender: String,
    val body: String,
    val matchedKeyword: String
)

@Serializable
data class GatewayPrefs(
    val url: String = "wss://reno-agent.tailf6416c.ts.net:443",
    val token: String = "8170f1b53202aaca279bfd6b133e18bdc367e4b9f5e5a05a"
)
