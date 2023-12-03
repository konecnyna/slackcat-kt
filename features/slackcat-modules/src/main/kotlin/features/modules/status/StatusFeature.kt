package features.chat.status

import data.chat.ChatGraph.chatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.FeatureGraph.featureCoroutineScope
import features.common.FeatureModule
import kotlinx.coroutines.launch

class StatusFeature : FeatureModule() {
    private val statusClient = StatusClient()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        featureCoroutineScope.launch {
            val response = statusClient.fetch()
            chatClient.sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channeId,
                    text = "Slack Status: ${response.status}"
                )
            )
        }
    }

    override fun provideCommand(): String = "status"
}
