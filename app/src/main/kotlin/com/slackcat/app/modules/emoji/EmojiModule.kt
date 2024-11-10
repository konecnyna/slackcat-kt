package com.slackcat.app.modules.emoji

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.random.Random

class EmojiModule : SlackcatModule() {
    private val emojiClient = EmojiClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val emoji = emojiClient.fetchEmoji(incomingChatMessage.userText)
            ?: return postHelpMessage(incomingChatMessage.channelId)

        val jsonString = """
            {
            "blocks": [
                {
                    "type": "image",
                    "image_url": "$emoji",
                    "alt_text": "summon image"
                }
            ]
        }"""
        val json = Json.parseToJsonElement(jsonString)

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = emoji,
                blocks = json.jsonObject
            )
        )
    }

    override fun provideCommand(): String = "emoji"

    override fun help(): String = buildMessage {
        title("EmojiModule Help")
        text("Grab emoji from this fun <https://gist.githubusercontent.com/konecnyna/9968c5a3457b4ef39a824222269f82f3/raw/ae599516d75899365318ce882f5be165a5083d2f/emojis.json|fun list>")
    }

}
