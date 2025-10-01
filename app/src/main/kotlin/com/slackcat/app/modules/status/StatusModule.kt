package com.slackcat.app.modules.status

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text

class StatusModule : SlackcatModule() {
    private val statusClient = StatusClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val statusService = getStatusSource(incomingChatMessage.arguments)
        if (statusService == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val response = statusClient.fetch(statusService)

        val message = response?.let {
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(response.toMessage())
            )
        } ?: OutgoingChatMessage(
            channelId = incomingChatMessage.channelId,
            message = text("Got en error when trying fetch status...")
        )

        sendMessage(message)
    }

    override fun provideCommand(): String = "status"

    override fun help(): String = buildMessage {
        title("StatusModule Help")
        text("Quickly check slacks status page with ?status command.")
        text("Usage: ?status --github")

        val entries =
            StatusClient.Service.entries
                .map { "${it.label} (${it.arguments.joinToString(", ")})" }
                .joinToString(", ")
        text("Availiable services: $entries")
    }

    private fun getStatusSource(arguments: List<String>): StatusClient.Service? {
        return StatusClient.Service.entries.find { service ->
            service.arguments.any { arg -> arguments.contains(arg) }
        }
    }
}
