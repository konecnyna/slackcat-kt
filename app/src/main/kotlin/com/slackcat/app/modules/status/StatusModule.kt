package com.slackcat.app.modules.status

import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import kotlinx.coroutines.launch

class StatusModule : SlackcatModule() {
    private val statusClient = StatusClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val statusService = getStatusSource(incomingChatMessage.arguments)
            ?: return postHelpMessage(incomingChatMessage.channelId)

        val response = statusClient.fetch(statusService)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = response.toMessage(),
            ),
        )
    }

    override fun provideCommand(): String = "status"

    override fun help(): String = buildMessage {
        title("StatusModule Help")
        text("Quickly check slacks status page with ?status command.")
        text("Usage: ?status --github")
    }


    private fun getStatusSource(arguments: List<String>): StatusClient.Service? {
        return StatusClient.Service.entries.find { service ->
            service.argument.any { arg -> arguments.contains(arg) }
        }
    }
}
