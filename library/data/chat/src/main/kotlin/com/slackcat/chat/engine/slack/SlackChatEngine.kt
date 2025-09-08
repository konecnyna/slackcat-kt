package com.slackcat.chat.engine.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.MessageBotEvent
import com.slack.api.model.event.MessageEvent
import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class SlackChatEngine(private val globalCoroutineScope: CoroutineScope) : ChatEngine {
    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    private val app = App()
    private val client = app.client

    override fun connect(ready: () -> Unit) {
        app.event(MessageBotEvent::class.java) { _, ctx ->
            // No - op. Need to handle it from the logs
            ctx.ack()
        }

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

        val socketModeApp = SocketModeApp(System.getenv("SLACK_APP_TOKEN"), app)
        globalCoroutineScope.launch {
            socketModeApp.startAsync()
            delay(2000)
            ready()
        }
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        val messageBlocks = if (message.message.text.isNotEmpty()) {
            val jsonObjectConverter = JsonToBlockConverter()
            jsonObjectConverter.jsonObjectToBlocks(message.message.text)
        } else {
            null
        }

        client.chatPostMessage { req ->
            req.apply {
                channel(message.channelId)
                blocks(messageBlocks)
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
