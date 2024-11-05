package data.chat.engine.slack

import app.AppGraph
import com.slack.api.bolt.App
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.AppMentionEvent
import com.slack.api.model.event.MessageEvent
import data.chat.engine.ChatEngine
import data.chat.models.ChatUser
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.WebSocket


class SlackChatEngine : ChatEngine {
    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    val messagesFlow = _messagesFlow.asSharedFlow()

    private val app = App()
    private val client = app.client

    override suspend fun connect() {
        app.event(MessageEvent::class.java) { payload, ctx ->
            val message = payload.event

            if (message.botId != null) {
                return@event ctx.ack()
            }
            println("""
            |New message:
            |Channel: ${message.channel}
            |User: ${message.user}
            |Text: ${message.text}
            |Timestamp: ${message.ts}
            |""".trimMargin())

            AppGraph.globalScope.launch {
                _messagesFlow.emit(message.toDomain())
            }
            ctx.ack()
        }

        val socketModeApp = SocketModeApp(app)
        AppGraph.globalScope.launch {
            socketModeApp.start()
        }
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        client.chatPostMessage { req ->
            req.channel(message.channel)
                .text(message.text)
        }
    }

    override suspend fun disconnect() {
        // TODO.
    }

    override suspend fun eventFlow() = messagesFlow
    override fun provideEngineName(): String = "SlackRTM"


    fun MessageEvent.toDomain() = IncomingChatMessage(
        channelId = channel,
        chatUser = ChatUser(userId = user),
        messageId = ts,
        rawMessage = text,
        userText = text,
        threadId = ts,
    )
}