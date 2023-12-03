package features.slackcat.models

import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage


abstract class SlackcatModule {
    abstract fun onInvoke(incomingChatMessage: IncomingChatMessage)
    abstract fun provideCommand(): String

    fun sendMessage(message: OutgoingChatMessage) {
        println(message)
    }
}

