package com.slackcat.modules.network.summon

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient
import kotlin.random.Random

class SummonModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val summonClient by lazy { SummonClient(networkClient) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        if (incomingChatMessage.userText.isEmpty()) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val images =
            summonClient.getHtml(
                incomingChatMessage.userText,
                incomingChatMessage.command == "gif",
            ).take(10)

        val imageUrl =
            when {
                images.isEmpty() -> "No results found found for `${incomingChatMessage.userText}`"
                incomingChatMessage.arguments.contains("--random") -> images[Random.nextInt(images.size)].image
                else -> images[0].image
            }

        val result =
            sendMessage(
                incomingMessage = incomingChatMessage,
                content =
                    buildMessage {
                        image(
                            url = imageUrl,
                            altText = "summon image: ${incomingChatMessage.userText}",
                        )
                    },
                preserveThreadContext = true,
            )

        result.fold(
            onSuccess = {
                // Message sent successfully, nothing to do
            },
            onFailure = { error ->
                // Send fallback message as plain text, preserving thread context
                sendMessage(
                    incomingMessage = incomingChatMessage,
                    content = textMessage("<$imageUrl>"),
                    preserveThreadContext = true,
                )
            },
        )
    }

    override fun commandInfo() =
        CommandInfo(
            command = "summon",
            aliases = SummonModuleAliases.entries.map { it.alias },
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("Summon Help")
            text("Summon an image from Google images. Use with caution...")
            text("Usage: ?summon slackcat")
        }
}

enum class SummonModuleAliases(val alias: String) {
    Gif("gif"),
}
