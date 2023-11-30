package features.chat.status

import kotlinx.coroutines.launch
import app.AppGraph.chatClient
import app.AppGraph.globalScope
import app.models.Message
import features.common.FeatureModule

class StatusFeature : FeatureModule() {
    private val statusClient = StatusClient()

    override fun onInvoke(message: Message) {
        globalScope.launch {
            val response = statusClient.fetch()
            println("Network: $response")
            chatClient.sendMessage("Slack Status: ${response.status}")
        }
    }


    override fun provideCommand(): String = "status"
}

