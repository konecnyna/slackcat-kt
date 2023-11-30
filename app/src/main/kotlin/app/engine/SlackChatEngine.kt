package app.engine

import app.App
import app.AppGraph.globalScope
import com.slack.api.Slack
import com.slack.api.rtm.RTMClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class SlackChatEngine : ChatEngine {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    private val _messagesFlow = MutableSharedFlow<String>()
    val messagesFlow = _messagesFlow.asSharedFlow()
    private val webSocketUrl = "wss://slack-rtm-api-url"

    override suspend fun connect() {
        val request = Request.Builder().url(webSocketUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                println("Connected to Slack RTM")
                // Publish flow here
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                globalScope.launch { _messagesFlow.emit(text) }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                globalScope.launch { _messagesFlow.emit(bytes.toString()) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                println("Closing Slack RTM: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                println("Error connecting to Slack RTM: ${t.message}")
            }
        })
    }

    override suspend fun sendMessage(message: String) {
        webSocket?.send(message) ?: println("WebSocket not connected")
    }

    override suspend fun disconnect() {
        webSocket?.close(1000, "Client done")
    }

    override suspend fun eventFlow() = messagesFlow
    override fun provideEngineName(): String = "SlackRTM"
}