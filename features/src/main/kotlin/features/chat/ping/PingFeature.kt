package features.chat.ping

import chat.ChatGraph.chatClient
import chat.models.IncomingChatMessage
import chat.models.OutgoingChatMessage
import features.common.FeatureModule

class PingFeature : FeatureModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        chatClient.sendMessage(OutgoingChatMessage("pong"))
    }

    override fun provideCommand(): String = "ping"
}