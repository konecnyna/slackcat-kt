package com.slackcat.app.modules.status

import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import kotlinx.coroutines.launch

class StatusModule : SlackcatModule() {
    private val statusClient = StatusClient()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        globalScope.launch {
            val response = statusClient.fetch()
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    text = "Slack Status: ${response.status}",
                ),
            )
        }
    }

    override fun provideCommand(): String = "status"

    override fun help(): String =
        buildMessage {
            title("StatusModule Help")
            text("Quickly check slacks status page with ?status command.")
        }
}
