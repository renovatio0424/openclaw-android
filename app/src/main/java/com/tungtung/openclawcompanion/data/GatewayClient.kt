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

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var currentToken: String = ""

    @Synchronized
    fun connect(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
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

        // onOpen: 아무것도 보내지 않음 — 게이트웨이가 먼저 challenge를 보낼 때까지 대기
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // 게이트웨이가 connect.challenge 이벤트를 먼저 보냄
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching {
                val obj = json.parseToJsonElement(text).jsonObject
                val type = obj["type"]?.jsonPrimitive?.content
                val event = obj["event"]?.jsonPrimitive?.content
                val ok = obj["ok"]?.jsonPrimitive?.content

                when {
                    // 1) 게이트웨이 → 클라이언트: challenge 수신 → connect 전송
                    type == "event" && event == "connect.challenge" -> {
                        webSocket.send(buildHandshake(token))
                    }
                    // 2) 게이트웨이 → 클라이언트: hello-ok (connect 성공)
                    type == "res" && ok == "true" -> {
                        _state.value = ConnectionState.CONNECTED
                    }
                    // 3) 인증 실패
                    type == "res" && ok == "false" -> {
                        _state.value = ConnectionState.ERROR
                        webSocket.close(1000, "auth failed")
                    }
                    else -> { /* 기타 메시지 무시 */ }
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) { /* 무시 */ }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = ConnectionState.ERROR
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
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
