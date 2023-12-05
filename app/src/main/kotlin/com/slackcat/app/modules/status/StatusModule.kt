package com.slackcat.app.modules.status

import com.features.slackcat.models.SlackcatModule
import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.launch

class StatusModule : SlackcatModule() {
    private val statusClient = StatusClient()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        globalScope.launch {
            val response = statusClient.fetch()
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channeId,
                    text = "Slack Status: ${response.status}",
                ),
            )
        }
    }

    override fun provideCommand(): String = "status"
}
