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

    // Cache for message_ts -> thread_ts mappings to avoid API calls on reactions
    // TTL: 24 hours (reactions typically happen on recent messages)
    private data class ThreadCacheEntry(val threadTs: String?, val timestamp: Long)

    private val threadCache = mutableMapOf<String, ThreadCacheEntry>()
    private val cacheTtlMs = 24 * 60 * 60 * 1000L // 24 hours

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
                // Cache the thread mapping (message_ts -> thread_ts)
                cacheThreadMapping(message.ts, message.threadTs)

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
                // Resolve thread root: check cache first, then API if needed
                val threadRoot = resolveThreadRoot(event.item.channel, event.item.ts)

                eventsFlow?.emit(
                    SlackcatEvent.ReactionAdded(
                        userId = event.user,
                        reaction = event.reaction,
                        channelId = event.item.channel,
                        messageTimestamp = event.item.ts,
                        threadTimestamp = threadRoot,
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
    ): Result<String> {
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

            val response =
                client.chatUpdate { req ->
                    req.apply {
                        channel(channelId)
                        ts(messageTs)
                        blocks(messageBlocks)
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
            ChatCapability.MESSAGE_UPDATES,
        )
    }

    override fun setEventsFlow(eventsFlow: MutableSharedFlow<SlackcatEvent>) {
        this.eventsFlow = eventsFlow
    }

    suspend fun getUserDisplayName(userId: String): Result<String> {
        return try {
            val response =
                client.usersInfo { req ->
                    req.user(userId)
                }

            if (response.isOk && response.user != null) {
                val displayName =
                    response.user.profile?.displayName?.takeIf { it.isNotBlank() }
                        ?: response.user.realName
                        ?: response.user.name
                        ?: userId
                Result.success(displayName)
            } else {
                Result.failure(Exception("Failed to fetch user info: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cache thread mapping with TTL cleanup
     */
    private fun cacheThreadMapping(
        messageTs: String,
        threadTs: String?,
    ) {
        val now = System.currentTimeMillis()
        threadCache[messageTs] = ThreadCacheEntry(threadTs, now)

        // Cleanup expired entries (older than TTL)
        threadCache.entries.removeIf { (_, entry) ->
            now - entry.timestamp > cacheTtlMs
        }
    }

    /**
     * Resolve thread root for a message, checking cache first, then API
     * Returns the thread root timestamp, or null if it's a top-level message
     */
    private suspend fun resolveThreadRoot(
        channelId: String,
        messageTs: String,
    ): String? {
        // Check cache first
        val cached = threadCache[messageTs]
        if (cached != null) {
            return cached.threadTs
        }

        // Cache miss - fetch from API
        return try {
            val response =
                client.conversationsHistory { req ->
                    req.channel(channelId)
                        .latest(messageTs)
                        .inclusive(true)
                        .limit(1)
                }

            if (response.isOk && response.messages.isNotEmpty()) {
                val message = response.messages[0]
                val threadTs = message.threadTs

                // Cache the result for future lookups
                cacheThreadMapping(messageTs, threadTs)

                threadTs
            } else {
                null
            }
        } catch (e: Exception) {
            println("[SlackChatEngine] Failed to fetch thread info for $messageTs: ${e.message}")
            null
        }
    }

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
