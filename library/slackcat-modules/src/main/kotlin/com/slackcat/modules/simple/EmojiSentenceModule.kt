package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import kotlin.random.Random

class EmojiSentenceModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val text = incomingChatMessage.userText
        val outgoingText = convertString(text)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(outgoingText),
            ),
        )
    }

    private fun convertString(userText: String): String {
        val converted = userText.lowercase().map { char ->
            if (char in 'a'..'z') {
                val color = if (Random.nextBoolean()) "white" else "yellow"
                ":alphabet-$color-$char:"
            } else {
                char.toString()
            }
        }.joinToString("")
        return converted
    }

    override fun help(): String = buildMessage {
        title("Emojisentence Help")
        text("Write some text braaa (Example '?emojisentence what up braaa')")
    }

    override fun provideCommand(): String = "emojisetence"
}
