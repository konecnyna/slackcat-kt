package com.slackcat.app.modules.status

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.slackcat.models.SlackcatModule

class StatusModule : SlackcatModule() {
    //private val statusClient = StatusClient()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
//        featureCoroutineScope.launch {
//            val response = statusClient.fetch()
//            chatClient.sendMessage(
//                OutgoingChatMessage(
//                    channelId = incomingChatMessage.channeId,
//                    text = "Slack Status: ${response.status}"
//                )
//            )
//        }
    }

    override fun provideCommand(): String = "status"
}
