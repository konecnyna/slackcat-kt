package com.slackcat.chat.engine.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.Attachment
import com.slack.api.model.event.MessageBotEvent
import com.slack.api.model.event.MessageChangedEvent
import com.slack.api.model.event.MessageChannelArchiveEvent
import com.slack.api.model.event.MessageChannelConvertToPublicEvent
import com.slack.api.model.event.MessageChannelJoinEvent
import com.slack.api.model.event.MessageChannelLeaveEvent
import com.slack.api.model.event.MessageChannelNameEvent
import com.slack.api.model.event.MessageChannelPostingPermissionsEvent
import com.slack.api.model.event.MessageChannelPurposeEvent
import com.slack.api.model.event.MessageChannelTopicEvent
import com.slack.api.model.event.MessageChannelUnarchiveEvent
import com.slack.api.model.event.MessageDeletedEvent
import com.slack.api.model.event.MessageEkmAccessDeniedEvent
import com.slack.api.model.event.MessageEvent
import com.slack.api.model.event.MessageFileShareEvent
import com.slack.api.model.event.MessageGroupTopicEvent
import com.slack.api.model.event.MessageMeEvent
import com.slack.api.model.event.MessageMetadataDeletedEvent
import com.slack.api.model.event.MessageMetadataPostedEvent
import com.slack.api.model.event.MessageMetadataUpdatedEvent
import com.slack.api.model.event.MessageRepliedEvent
import com.slack.api.model.event.MessageThreadBroadcastEvent
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
    private val threadCache = SlackThreadCache(client)
    private val apiOps = SlackApiOperations(client)

    override fun connect(ready: () -> Unit) {
        app.event(MessageBotEvent::class.java) { payload, ctx ->
            val message = payload.event
            emitBotMessage(
                botId = message.botId ?: "",
                channelId = message.channel,
                text = message.text ?: "",
                timestamp = message.ts,
                threadTimestamp = message.threadTs,
                attachmentText = extractAttachmentText(message.attachments),
            )
            ctx.ack()
        }

        app.event(MessageEvent::class.java) { payload, ctx ->
            val message = payload.event
            if (message.botId != null) {
                emitBotMessage(
                    botId = message.botId,
                    channelId = message.channel,
                    text = message.text,
                    timestamp = message.ts,
                    threadTimestamp = message.threadTs,
                    attachmentText = extractAttachmentText(message.attachments),
                )
                return@event ctx.ack()
            }
            handleUserMessage(message)
            ctx.ack()
        }

        app.event(ReactionAddedEvent::class.java) { payload, ctx ->
            val event = payload.event
            emitEvent {
                val threadRoot = threadCache.resolveThreadRoot(event.item.channel, event.item.ts)
                SlackcatEvent.ReactionAdded(
                    userId = event.user,
                    reaction = event.reaction,
                    channelId = event.item.channel,
                    messageTimestamp = event.item.ts,
                    threadTimestamp = threadRoot,
                    itemUserId = event.itemUser,
                    eventTimestamp = event.eventTs,
                )
            }
            ctx.ack()
        }

        app.event(ReactionRemovedEvent::class.java) { payload, ctx ->
            val event = payload.event
            emitEvent {
                SlackcatEvent.ReactionRemoved(
                    userId = event.user,
                    reaction = event.reaction,
                    channelId = event.item.channel,
                    messageTimestamp = event.item.ts,
                    itemUserId = event.itemUser,
                    eventTimestamp = event.eventTs,
                )
            }
            ctx.ack()
        }

        // Bolt routes message subtypes (channel_join, message_changed, etc.) to dedicated event
        // classes. Without registered handlers, Bolt returns 404 and Slack retries — repeated
        // 404s cause Slack to auto-disable Event Subscriptions. Ack the subtypes we don't act on.
        listOf(
            MessageChangedEvent::class.java,
            MessageChannelArchiveEvent::class.java,
            MessageChannelConvertToPublicEvent::class.java,
            MessageChannelJoinEvent::class.java,
            MessageChannelLeaveEvent::class.java,
            MessageChannelNameEvent::class.java,
            MessageChannelPostingPermissionsEvent::class.java,
            MessageChannelPurposeEvent::class.java,
            MessageChannelTopicEvent::class.java,
            MessageChannelUnarchiveEvent::class.java,
            MessageDeletedEvent::class.java,
            MessageEkmAccessDeniedEvent::class.java,
            MessageFileShareEvent::class.java,
            MessageGroupTopicEvent::class.java,
            MessageMeEvent::class.java,
            MessageMetadataDeletedEvent::class.java,
            MessageMetadataPostedEvent::class.java,
            MessageMetadataUpdatedEvent::class.java,
            MessageRepliedEvent::class.java,
            MessageThreadBroadcastEvent::class.java,
        ).forEach { eventClass ->
            app.event(eventClass) { _, ctx -> ctx.ack() }
        }

        val socketModeApp = SocketModeApp(System.getenv("SLACK_APP_TOKEN"), app)
        globalCoroutineScope.launch {
            socketModeApp.startAsync()
            delay(2000)
            ready()
        }
    }

    private fun emitEvent(block: suspend () -> SlackcatEvent) {
        globalCoroutineScope.launch {
            eventsFlow?.emit(block())
        }
    }

    private fun extractAttachmentText(attachments: List<Attachment>?): String {
        if (attachments.isNullOrEmpty()) return ""
        return attachments.mapNotNull { attachment ->
            attachment.fallback ?: attachment.text ?: attachment.pretext ?: attachment.title
        }.joinToString("\n")
    }

    private fun emitBotMessage(
        botId: String,
        channelId: String,
        text: String,
        timestamp: String,
        threadTimestamp: String?,
        attachmentText: String = "",
    ) {
        emitEvent {
            SlackcatEvent.BotMessageReceived(
                botId = botId,
                channelId = channelId,
                text = text,
                timestamp = timestamp,
                threadTimestamp = threadTimestamp,
                attachmentText = attachmentText,
            )
        }
    }

    private fun handleUserMessage(message: MessageEvent) {
        globalCoroutineScope.launch {
            threadCache.cacheThreadMapping(message.ts, message.threadTs)

            eventsFlow?.emit(
                SlackcatEvent.MessageReceived(
                    userId = message.user,
                    channelId = message.channel,
                    text = message.text,
                    timestamp = message.ts,
                    threadTimestamp = message.threadTs,
                ),
            )

            CommandParser.extractCommand(message.text)?.let {
                _messagesFlow.emit(message.toDomain(it))
            }
        }
    }

    override suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<String> {
        return try {
            val response =
                if (message.plainText) {
                    client.chatPostMessage { req ->
                        req.apply {
                            channel(message.channelId)
                            text(toPlainText(message.content))
                            username(botName)
                            message.threadId?.let { threadTs(it) }
                            when (botIcon) {
                                is BotIcon.BotEmojiIcon -> iconEmoji(botIcon.emoji)
                                is BotIcon.BotImageIcon -> iconUrl(botIcon.url)
                            }
                        }
                    }
                } else {
                    val messageBlocks = messageConverter.toSlackBlocks(message.content)
                    val color = messageConverter.toColorString(message.content.style)

                    client.chatPostMessage { req ->
                        req.apply {
                            channel(message.channelId)
                            if (color != null) {
                                val attachment =
                                    Attachment.builder()
                                        .color(color)
                                        .blocks(messageBlocks)
                                        .build()
                                attachments(listOf(attachment))
                            } else {
                                blocks(messageBlocks)
                            }
                            username(botName)
                            message.threadId?.let { threadTs(it) }
                            when (botIcon) {
                                is BotIcon.BotEmojiIcon -> iconEmoji(botIcon.emoji)
                                is BotIcon.BotImageIcon -> iconUrl(botIcon.url)
                            }
                        }
                    }
                }

            if (response.isOk) {
                Result.success(response.ts)
            } else {
                Result.failure(Exception("Slack API error: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMessage(
        channelId: String,
        messageTs: String,
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<String> {
        return try {
            val messageBlocks = messageConverter.toSlackBlocks(message.content)
            val color = messageConverter.toColorString(message.content.style)

            val response =
                client.chatUpdate { req ->
                    req.apply {
                        channel(channelId)
                        ts(messageTs)
                        if (color != null) {
                            val attachment =
                                Attachment.builder()
                                    .color(color)
                                    .blocks(messageBlocks)
                                    .build()
                            attachments(listOf(attachment))
                        } else {
                            blocks(messageBlocks)
                        }
                    }
                }

            if (response.isOk) {
                Result.success(response.ts)
            } else {
                Result.failure(Exception("Slack API error: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toPlainText(message: com.slackcat.common.BotMessage): String {
        return message.elements.joinToString("\n") { element ->
            when (element) {
                is com.slackcat.common.MessageElement.Text -> element.content
                is com.slackcat.common.MessageElement.Heading -> "*${element.content}*"
                is com.slackcat.common.MessageElement.Image -> "[Image: ${element.altText}]"
                is com.slackcat.common.MessageElement.Divider -> "---"
                is com.slackcat.common.MessageElement.KeyValueList ->
                    element.items.joinToString("\n") { "${it.key}: ${it.value}" }
                is com.slackcat.common.MessageElement.Context -> element.content
            }
        }
    }

    override suspend fun eventFlow() = messagesFlow

    override fun provideEngineName(): String = "SlackRTM"

    override fun capabilities(): Set<ChatCapability> {
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
            ChatCapability.MESSAGE_UPDATES,
        )
    }

    override fun setEventsFlow(eventsFlow: MutableSharedFlow<SlackcatEvent>) {
        this.eventsFlow = eventsFlow
    }

    suspend fun getMessageText(
        channelId: String,
        messageTs: String,
        threadTs: String?,
    ): Result<String> = apiOps.getMessageText(channelId, messageTs, threadTs)

    suspend fun getUserDisplayName(userId: String): Result<String> = apiOps.getUserDisplayName(userId)

    suspend fun getThreadRepliers(
        channelId: String,
        threadTs: String,
    ): Result<List<String>> = apiOps.getThreadRepliers(channelId, threadTs)

    suspend fun getUserGroupMembers(usergroupId: String): Result<List<String>> = apiOps.getUserGroupMembers(usergroupId)

    fun MessageEvent.toDomain(command: String) =
        IncomingChatMessage(
            command = command,
            channelId = channel,
            chatUser = ChatUser(userId = user),
            messageId = ts,
            rawMessage = text,
            threadId = threadTs,
            arguments = CommandParser.extractArguments(text),
            userText = CommandParser.extractUserText(text),
        )
}
