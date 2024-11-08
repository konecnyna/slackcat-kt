package com.slackcat.app.modules.ping

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage

class PingModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = "pong",
            ),
        )
    }

    override fun provideCommand(): String = "ping"

    override fun help(): String =
        buildMessage {
            title("Ping Help")
            text("This module is for debugging. If slackcat is running ?ping will return ?pong")
        }
}
