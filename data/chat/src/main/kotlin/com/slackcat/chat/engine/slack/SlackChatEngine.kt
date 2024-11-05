package com.slackcat.chat.engine.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.AppMentionEvent
import com.slack.api.model.event.MessageEvent
import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.WebSocket


class SlackChatEngine(val globalCoroutineScope: CoroutineScope) : ChatEngine {
    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    private val app = App()
    private val client = app.client

    override fun connect() {
        app.event(MessageEvent::class.java) { payload, ctx ->
            val message = payload.event

            if (message.botId != null) {
                return@event ctx.ack()
            }
            globalCoroutineScope.launch {
                _messagesFlow.emit(message.toDomain())
            }
            ctx.ack()
        }

        val socketModeApp = SocketModeApp(app)
        globalCoroutineScope.launch {
            socketModeApp.start()
        }
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        client.chatPostMessage { req ->
            req.channel(message.channelId)
                .text(message.text)
        }
    }

    override suspend fun eventFlow() = messagesFlow
    override fun provideEngineName(): String = "SlackRTM"


    fun MessageEvent.toDomain() = IncomingChatMessage(
        channeId = channel,
        chatUser = ChatUser(userId = user),
        messageId = ts,
        rawMessage = text,
        threadId = ts,
    )
}