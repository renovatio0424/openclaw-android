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
    val smsWhitelist: List<String> = emptyList(),
    val smsWhitelistAll: Boolean = false,
    val blockedKeywords: List<String> = emptyList()
) {
    companion object {
        val Default = FilterConfig()
    }
}

@Serializable
data class GatewayPrefs(
    val url: String = "ws://reno-agent.tailf6416c.ts.net:18789",
    val token: String = ""
)
