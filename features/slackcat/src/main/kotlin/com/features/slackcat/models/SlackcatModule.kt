package com.features.slackcat.models

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import org.jetbrains.exposed.sql.Table


abstract class SlackcatModule {
    abstract fun onInvoke(incomingChatMessage: IncomingChatMessage)
    abstract fun provideCommand(): String

    // Fix this.
    internal lateinit var chatClient: ChatClient
    fun sendMessage(message: OutgoingChatMessage) {
        chatClient.sendMessage(message)
    }
}

