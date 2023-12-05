package com.slackcat.app.modules.ping

import com.features.slackcat.models.SlackcatModule
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage

class PingModule : SlackcatModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channeId,
                text = "pong",
            ),
        )
    }

    override fun provideCommand(): String = "ping"
}
