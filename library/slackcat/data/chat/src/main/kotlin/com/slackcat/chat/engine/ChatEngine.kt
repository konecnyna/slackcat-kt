package com.slackcat.chat.engine

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.flow.SharedFlow

interface ChatEngine {
    fun connect(ready: () -> Unit)

    suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<Unit>

    suspend fun eventFlow(): SharedFlow<IncomingChatMessage>

    fun provideEngineName(): String
}
