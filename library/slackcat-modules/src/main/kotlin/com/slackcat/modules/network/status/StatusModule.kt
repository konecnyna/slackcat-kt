package com.slackcat.modules.network.status

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient

open class StatusModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val statusClient by lazy { StatusClient(networkClient) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val statusService = getStatusSource(incomingChatMessage.arguments, incomingChatMessage.userText)
        if (statusService == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val response = statusClient.fetch(statusService)

        val message =
            response?.let {
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content = it.toRichMessage(),
                )
            } ?: OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("Got an error when trying to fetch status..."),
            )

        sendMessage(message)
    }

    override fun commandInfo() = CommandInfo(command = "status")

    override fun help(): BotMessage =
        buildMessage {
            heading("Status Module Help")
            text(
                buildString {
                    appendLine("Check the status of various services.")
                    appendLine("\nUsage:")
                    appendLine("\u2022 `?status --github` or `?status github`")
                    appendLine("\u2022 `?status --slack` or `?status slack`")
                    appendLine("\n*Available services:*")

                    StatusClient.Service.entries.forEach { service ->
                        val args = service.arguments.joinToString(", ") { "`$it`" }
                        appendLine("\u2022 *${service.label}*: $args")
                    }
                },
            )
        }

    // Supports both --flag and direct text arguments (e.g., both "--github" and "github")
    private fun getStatusSource(
        arguments: List<String>,
        userText: String,
    ): StatusClient.Service? {
        val fromArguments =
            StatusClient.Service.entries.find { service ->
                service.arguments.any { arg -> arguments.contains(arg) }
            }

        if (fromArguments != null) {
            return fromArguments
        }

        val normalizedText = userText.trim().lowercase()
        return StatusClient.Service.entries.find { service ->
            service.arguments.any { arg ->
                arg.removePrefix("--").lowercase() == normalizedText
            }
        }
    }
}
