package features.chat.ping

import data.chat.ChatGraph.chatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.common.FeatureModule

class PingFeature : FeatureModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        chatClient.sendMessage(OutgoingChatMessage("pong"))
    }

    override fun provideCommand(): String = "ping"
}
