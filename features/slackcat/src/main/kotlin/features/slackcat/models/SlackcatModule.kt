package features.slackcat.models

import data.chat.models.ChatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
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

