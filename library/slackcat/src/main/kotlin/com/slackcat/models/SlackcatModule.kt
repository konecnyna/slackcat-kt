package com.slackcat.models

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.presentation.text
import kotlinx.coroutines.CoroutineScope

abstract class SlackcatModule {
    abstract suspend fun onInvoke(incomingChatMessage: IncomingChatMessage)

    abstract fun provideCommand(): String

    abstract fun help(): String

    // Fix this.
    internal lateinit var chatClient: ChatClient

    lateinit var coroutineScope: CoroutineScope

    suspend fun sendMessage(message: OutgoingChatMessage): Result<Unit> {
        return chatClient.sendMessage(message)
    }

    suspend fun postHelpMessage(channelId: String): Result<Unit> {
        return sendMessage(
            OutgoingChatMessage(
                channelId = channelId,
                message = text(help())
            ),
        )
    }

    open fun aliases(): List<String> = emptyList()
}
