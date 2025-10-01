package com.slackcat.app.modules.emoji

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage

class EmojiModule : SlackcatModule() {
    private val emojiClient = EmojiClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val emoji = emojiClient.fetchEmoji(incomingChatMessage.userText)
        if (emoji == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = buildRichMessage {
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
