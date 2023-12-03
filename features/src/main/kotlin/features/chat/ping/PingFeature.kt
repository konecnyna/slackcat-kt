package features.chat.ping

import data.chat.ChatGraph.chatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.common.FeatureModule

class PingFeature : FeatureModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        chatClient.sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channeId,
                text = "pong"
            )
        )
    }

    override fun provideCommand(): String = "ping"
}
