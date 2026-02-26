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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "GatewayClient"

object GatewayClient {
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var currentToken: String = ""

    @Synchronized
    fun connect(url: String, token: String) {
        Log.d(TAG, "connect() url=$url token=${token.take(8)}...")
        if (url.isBlank() || token.isBlank()) {
            Log.e(TAG, "connect() failed: url or token is blank")
            _state.value = ConnectionState.ERROR
            return
        }
        currentToken = token
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
        webSocket?.send(json.encodeToString(JsonObject.serializer(), payload))
    }

    private fun createListener(token: String) = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "onOpen: WebSocket connected, waiting for challenge")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching {
                val obj = json.parseToJsonElement(text).jsonObject
                val type = obj["type"]?.jsonPrimitive?.content
                val event = obj["event"]?.jsonPrimitive?.content
                val ok = obj["ok"]?.jsonPrimitive?.content

                Log.d(TAG, "onMessage: type=$type event=$event ok=$ok")
                when {
                    type == "event" && event == "connect.challenge" -> {
                        Log.i(TAG, "Received challenge, sending handshake")
                        webSocket.send(buildHandshake(token))
                    }
                    type == "res" && ok == "true" -> {
                        Log.i(TAG, "Connected successfully!")
                        _state.value = ConnectionState.CONNECTED
                    }
                    type == "res" && ok == "false" -> {
                        Log.e(TAG, "Auth failed: $text")
                        _state.value = ConnectionState.ERROR
                        webSocket.close(1000, "auth failed")
                    }
                    else -> { Log.d(TAG, "Unhandled message: $text") }
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) { /* 무시 */ }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "onFailure: ${t.message}", t)
            _state.value = ConnectionState.ERROR
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "onClosed: code=$code reason=$reason")
            _state.value = ConnectionState.DISCONNECTED
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
                    put("id", JsonPrimitive("openclaw-android"))
                    put("version", JsonPrimitive("1.0.0"))
                    put("platform", JsonPrimitive("android"))
                    put("mode", JsonPrimitive("ui"))
                })
                put("role", JsonPrimitive("operator"))
                put("scopes", buildJsonArray {
                    add(JsonPrimitive("operator.read"))
                    add(JsonPrimitive("operator.write"))
                })
                put("caps", buildJsonArray {})
                put("auth", buildJsonObject {
                    put("token", JsonPrimitive(token))
                })
                put("locale", JsonPrimitive("ko-KR"))
                put("userAgent", JsonPrimitive("openclaw-android/1.0.0"))
            })
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }
}
