package com.slackcat.app.modules.summon

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage

class SummonModule : SlackcatModule() {
    private val summonClient = SummonClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val statusService = getStatusSource(incomingChatMessage.arguments)
            ?: return postHelpMessage(incomingChatMessage.channelId)

        val response = summonClient.fetch(statusService)

        val message = response?.let {
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = response.toMessage(),
            )
        } ?: OutgoingChatMessage(
            channelId = incomingChatMessage.channelId,
            text = "Got en error when trying fetch status...",
        )

        sendMessage(message)
    }

    override fun provideCommand(): String = "status"

    override fun help(): String = buildMessage {
        title("StatusModule Help")
        text("Quickly check slacks status page with ?status command.")
        text("Usage: ?status --github")

        val entries =
            SummonClient.Service.entries
                .map { "${it.label} (${it.arguments.joinToString(", ")})" }
                .joinToString(", ")
        text("Availiable services: $entries")
    }

    private fun getStatusSource(arguments: List<String>): SummonClient.Service? {
        return SummonClient.Service.entries.find { service ->
            service.arguments.any { arg -> arguments.contains(arg) }
        }
    }
}
