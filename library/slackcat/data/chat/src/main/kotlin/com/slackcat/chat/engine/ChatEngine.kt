package com.slackcat.chat.engine

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.ChatCapability
import com.slackcat.common.SlackcatEvent
import kotlinx.coroutines.flow.SharedFlow

interface ChatEngine {
    fun connect(ready: () -> Unit)

    suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<Unit>

    /**
     * Deletes a message from a channel.
     * @param channelId The channel ID
     * @param timestamp The message timestamp
     * @return Result indicating success or failure
     */
    suspend fun deleteMessage(
        channelId: String,
        timestamp: String,
    ): Result<Unit>

    suspend fun eventFlow(): SharedFlow<IncomingChatMessage>

    fun provideEngineName(): String

    /**
     * Returns the set of capabilities this chat engine supports.
     * Modules can use this to adapt their behavior to different platforms.
     */
    fun capabilities(): Set<ChatCapability>

    /**
     * Set the events flow for emitting SlackcatEvents (like reactions).
     * This is called by SlackcatBot to wire up the event system.
     */
    fun setEventsFlow(eventsFlow: kotlinx.coroutines.flow.MutableSharedFlow<SlackcatEvent>)
}
