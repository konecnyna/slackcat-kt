package app.modules.ping

import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.slackcat.models.SlackcatModule

class PingModule : SlackcatModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channeId,
                text = "pong"
            )
        )
    }

    override fun provideCommand(): String = "ping"
}
