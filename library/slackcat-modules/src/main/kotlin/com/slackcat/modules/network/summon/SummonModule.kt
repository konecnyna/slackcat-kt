package com.slackcat.modules.network.summon

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildRichMessage
import com.slackcat.presentation.text
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
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    message =
                        buildRichMessage {
                            image(
                                imageUrl = imageUrl,
                                altText = "summon image: ${incomingChatMessage.userText}",
                            )
                            context("Source: $imageUrl")
                        },
                ),
            )

        result.fold(
            onSuccess = {
                // Message sent successfully, nothing to do
            },
            onFailure = { error ->
                // Send fallback message as plain text in a thread
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        threadId = incomingChatMessage.messageId,
                        message = text("<$imageUrl>"),
                    ),
                )
            },
        )
    }

    override fun provideCommand(): String = "summon"

    override fun aliases(): List<String> = SummonModuleAliases.entries.map { it.alias }

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
