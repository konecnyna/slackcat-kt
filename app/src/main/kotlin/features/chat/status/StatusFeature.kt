package features.chat.status

import app.App
import app.Router
import features.common.ChatModule
import kotlinx.coroutines.launch

class StatusFeature : ChatModule() {
    val statusClient = StatusClient()

    override fun onInvoke(message: Router.Message) {
        App.globalScope.launch {
            val response = statusClient.fetch()
            println("Network: $response")
            chatClient.sendMessage("Slack Status: ${response.status}")
        }
    }


    override fun provideCommand(): String = "status"
}