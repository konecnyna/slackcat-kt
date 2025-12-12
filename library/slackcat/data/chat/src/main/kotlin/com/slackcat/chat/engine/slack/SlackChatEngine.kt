package com.slackcat.chat.engine.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.MessageBotEvent
import com.slack.api.model.event.MessageEvent
import com.slack.api.model.event.ReactionAddedEvent
import com.slack.api.model.event.ReactionRemovedEvent
import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.ChatCapability
import com.slackcat.common.CommandParser
import com.slackcat.common.SlackcatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SlackChatEngine(private val globalCoroutineScope: CoroutineScope) : ChatEngine {
    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    private var eventsFlow: MutableSharedFlow<SlackcatEvent>? = null

    private val app = App()
    private val client = app.client
    private val messageConverter = SlackMessageConverter()

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
                // Emit ALL messages to event listeners (for features like timeout)
                eventsFlow?.emit(
                    SlackcatEvent.MessageReceived(
                        userId = message.user,
                        channelId = message.channel,
                        text = message.text,
                        timestamp = message.ts,
                        threadTimestamp = message.threadTs,
                    ),
                )

                // Also emit commands to the command flow
                CommandParser.extractCommand(message.text)?.let {
                    _messagesFlow.emit(message.toDomain(it))
                }
            }
            ctx.ack()
        }

        app.event(ReactionAddedEvent::class.java) { payload, ctx ->
            val event = payload.event
            globalCoroutineScope.launch {
                eventsFlow?.emit(
                    SlackcatEvent.ReactionAdded(
                        userId = event.user,
                        reaction = event.reaction,
                        channelId = event.item.channel,
                        messageTimestamp = event.item.ts,
                        itemUserId = event.itemUser,
                        eventTimestamp = event.eventTs,
                    ),
                )
            }
            ctx.ack()
        }

        app.event(ReactionRemovedEvent::class.java) { payload, ctx ->
            val event = payload.event
            globalCoroutineScope.launch {
                eventsFlow?.emit(
                    SlackcatEvent.ReactionRemoved(
                        userId = event.user,
                        reaction = event.reaction,
                        channelId = event.item.channel,
                        messageTimestamp = event.item.ts,
                        itemUserId = event.itemUser,
                        eventTimestamp = event.eventTs,
                    ),
                )
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

    override suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<Unit> {
        return try {
            val messageBlocks = messageConverter.toSlackBlocks(message.content)

            val response =
                client.chatPostMessage { req ->
                    req.apply {
                        channel(message.channelId)
                        blocks(messageBlocks)
                        username(botName)
                        message.threadId?.let { threadTs(it) }
                        when (botIcon) {
                            is BotIcon.BotEmojiIcon -> iconEmoji(botIcon.emoji)
                            is BotIcon.BotImageIcon -> iconUrl(botIcon.url)
                        }
                    }
                }

            if (response.isOk) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Slack API error: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eventFlow() = messagesFlow

    override fun provideEngineName(): String = "SlackRTM"

    override fun capabilities(): Set<ChatCapability> {
        // Slack supports almost all capabilities
        return setOf(
            ChatCapability.RICH_FORMATTING,
            ChatCapability.IMAGES,
            ChatCapability.THUMBNAILS,
            ChatCapability.DIVIDERS,
            ChatCapability.STRUCTURED_FIELDS,
            ChatCapability.THREADS,
            ChatCapability.REACTIONS,
            ChatCapability.CUSTOM_BOT_ICON,
            ChatCapability.CUSTOM_BOT_NAME,
            ChatCapability.MESSAGE_COLORS,
        )
    }

    override fun setEventsFlow(eventsFlow: MutableSharedFlow<SlackcatEvent>) {
        this.eventsFlow = eventsFlow
    }

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
