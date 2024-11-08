package com.slackcat.chat.engine.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.MessageEvent
import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class SlackChatEngine(private val globalCoroutineScope: CoroutineScope) : ChatEngine {
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
                CommandParser.extractCommand(message.text)?.let {
                    _messagesFlow.emit(message.toDomain(it))
                }
            }
            ctx.ack()
        }

        val socketModeApp = SocketModeApp(app)
        globalCoroutineScope.launch {
            socketModeApp.start()
        }
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        val jsonObjectConverter = JsonToBlockConverter()
        val blocks = jsonObjectConverter.jsonObjectToBlocks(message.blocks)
        client.chatPostMessage { req ->
            req.apply {
                channel(message.channelId)
                text(message.text)
                blocks(blocks)
                username(message.botName)
                when (val icon = message.botIcon) {
                    is BotIcon.BotEmojiIcon -> iconEmoji(icon.emoji)
                    is BotIcon.BotImageIcon -> iconUrl(icon.url)
                }
            }
        }
    }

    override suspend fun eventFlow() = messagesFlow

    override fun provideEngineName(): String = "SlackRTM"

    fun MessageEvent.toDomain(command: String) =
        IncomingChatMessage(
            command = command,
            channelId = channel,
            chatUser = ChatUser(userId = user),
            messageId = ts,
            rawMessage = text,
            threadId = ts,
            arguments = CommandParser.extractArguments(text),
            userText = CommandParser.extractUserText(text),
        )
}
