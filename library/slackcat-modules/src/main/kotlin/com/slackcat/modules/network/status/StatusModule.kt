package com.slackcat.modules.network.status

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient

class StatusModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val statusClient by lazy { StatusClient(networkClient) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val statusService = getStatusSource(incomingChatMessage.arguments)
        if (statusService == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val response = statusClient.fetch(statusService)
        val message = response?.toMessage() ?: "Got an error when trying to fetch status..."

        sendMessage(
            OutgoingChatMessage.ChannelMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage(message),
            ),
        )
    }

    override fun commandInfo() = CommandInfo(command = "status")

    override fun help(): BotMessage =
        buildMessage {
            heading("StatusModule Help")
            text(
                "Check service status pages. Usage: ?status <service>\n" +
                    "Services: ${
                        StatusClient.Service.entries.joinToString(", ") {
                            "${it.label} (${it.arguments.joinToString(", ")})"
                        }
                    }",
            )
        }

    private fun getStatusSource(arguments: List<String>): StatusClient.Service? {
        return StatusClient.Service.entries.find { service ->
            service.arguments.any { arg -> arguments.contains(arg) }
        }
    }
}
