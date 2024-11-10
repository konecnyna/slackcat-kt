package com.slackcat.app.modules.summon

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.RichTextMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.RichMessage
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.random.Random

class SummonModule : SlackcatModule() {
    private val summonClient = SummonClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        if (incomingChatMessage.userText.isEmpty()) {
            return postHelpMessage(incomingChatMessage.channelId)
        }

        val images = summonClient.getHtml(incomingChatMessage.userText, incomingChatMessage.command == "gif")
        val message = if (images.isEmpty()) {
            "No results found found for `${incomingChatMessage.userText}`"
        } else {
            images[Random.nextInt(images.size)].image
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                richText =  buildRichMessage {
                    image(
                        imageUrl = images[Random.nextInt(images.size)].image,
                        altText = "summon image"
                    )
                }
            )
        )
    }

    override fun provideCommand(): String = "summon"
    override fun aliases(): List<String> = SummonModuleAliases.entries.map { it.alias }

    override fun help(): String = buildMessage {
        title("Summon Help")
        text("Summon an image from Google images. Use with caution...")
        text("Usage: ?summon slackcat")
    }

}


enum class SummonModuleAliases(val alias: String) {
    Gif("gif")
}
