package com.slackcat.app.modules.emoji

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class EmojiModule : SlackcatModule() {
    private val emojiClient = EmojiClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val emoji = emojiClient.fetchEmoji(incomingChatMessage.userText)
            ?: return postHelpMessage(incomingChatMessage.channelId)

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                richText = buildRichMessage {
                    image(
                        imageUrl = emoji,
                        altText = "summon image"
                    )
                }
            )
        )
    }

    override fun provideCommand(): String = "emoji"

    override fun help(): String = buildMessage {
        title("EmojiModule Help")
        text("Grab emoji from this fun <https://gist.github.com/konecnyna/9968c5a3457b4ef39a824222269f82f3|fun list>")
    }

}
