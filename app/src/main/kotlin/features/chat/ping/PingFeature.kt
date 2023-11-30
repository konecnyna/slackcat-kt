package features.chat.ping

import app.Router
import features.common.ChatModule

class PingFeature : ChatModule() {
    override fun onInvoke(message: Router.Message) {
        chatClient.sendMessage("pong")
    }

    override fun provideCommand(): String = "ping"
}