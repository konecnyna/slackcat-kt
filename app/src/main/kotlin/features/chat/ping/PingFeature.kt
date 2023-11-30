package features.chat.ping

import app.AppGraph.chatClient
import app.models.Message
import features.common.FeatureModule

class PingFeature : FeatureModule() {
    override fun onInvoke(message: Message) {
        chatClient.sendMessage("pong")
    }

    override fun provideCommand(): String = "ping"
}