package features.ping

import app.Router
import features.ChatModule

class PingFeature : ChatModule() {
    override fun onInvoke(message: Router.Message) {
        chatClient.sendMessage("pong")
    }

    override fun provideCommand(): String = "ping"
}