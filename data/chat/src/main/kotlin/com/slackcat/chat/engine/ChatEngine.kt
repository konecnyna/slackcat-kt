package com.slackcat.chat.engine

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.flow.SharedFlow

interface ChatEngine {
    fun connect()

    suspend fun sendMessage(message: OutgoingChatMessage)

    suspend fun eventFlow(): SharedFlow<IncomingChatMessage>

    fun provideEngineName(): String
}
