package com.tungtung.openclawcompanion.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.TimeUnit

object GatewayClient {
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { encodeDefaults = true }

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Synchronized
    fun connect(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            _state.value = ConnectionState.ERROR
            return
        }
        _state.value = ConnectionState.CONNECTING
        webSocket?.cancel()
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, createListener(token))
    }

    @Synchronized
    fun disconnect() {
        webSocket?.close(1000, "client requested")
        webSocket = null
        _state.value = ConnectionState.DISCONNECTED
    }

    fun sendEvent(type: String, source: String, text: String) {
        val payload = buildJsonObject {
            put("type", JsonPrimitive("req"))
            put("id", JsonPrimitive(UUID.randomUUID().toString()))
            put("method", JsonPrimitive("chat.send"))
            put("params", buildJsonObject {
                put("text", JsonPrimitive("[$type][$source] $text"))
                put("sessionKey", JsonPrimitive("main"))
            })
        }
        val message = json.encodeToString(JsonObject.serializer(), payload)
        webSocket?.send(message)
    }

    private fun createListener(token: String) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _state.value = ConnectionState.CONNECTED
            webSocket.send(buildHandshake(token))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = ConnectionState.ERROR
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = ConnectionState.DISCONNECTED
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // Ack not required
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // Responses ignored for now
        }
    }

    private fun buildHandshake(token: String): String {
        val payload = buildJsonObject {
            put("type", JsonPrimitive("req"))
            put("id", JsonPrimitive(UUID.randomUUID().toString()))
            put("method", JsonPrimitive("connect"))
            put("params", buildJsonObject {
                put("minProtocol", JsonPrimitive(3))
                put("maxProtocol", JsonPrimitive(3))
                put("client", buildJsonObject {
                    put("id", JsonPrimitive("android-companion"))
                    put("version", JsonPrimitive("1.0.0"))
                    put("platform", JsonPrimitive("android"))
                    put("mode", JsonPrimitive("operator"))
                })
                put("role", JsonPrimitive("operator"))
                put("scopes", buildJsonArray {
                    add(JsonPrimitive("operator.read"))
                    add(JsonPrimitive("operator.write"))
                })
                put("auth", buildJsonObject {
                    put("token", JsonPrimitive(token))
                })
                put("userAgent", JsonPrimitive("openclaw-android/1.0.0"))
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }
}
