package features.chat.ping

import app.AppGraph.chatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.common.FeatureModule

class PingFeature : FeatureModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        chatClient.sendMessage(
            OutgoingChatMessage(
                channel = incomingChatMessage.channelId,
                text = "pong"
            )
        )
    }

    override fun provideCommand(): String = "ping"
}