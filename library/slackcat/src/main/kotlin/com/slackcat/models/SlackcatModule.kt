package com.slackcat.models

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.presentation.text

abstract class SlackcatModule {
    abstract suspend fun onInvoke(incomingChatMessage: IncomingChatMessage)

    abstract fun provideCommand(): String

    abstract fun help(): String

    // Fix this.
    internal lateinit var chatClient: ChatClient

    fun sendMessage(message: OutgoingChatMessage) {
        chatClient.sendMessage(message)
    }

    fun postHelpMessage(channelId: String) {
        sendMessage(
            OutgoingChatMessage(
                channelId = channelId,
                message = text(help())
            ),
        )
    }

    open fun aliases(): List<String> = emptyList()
}
