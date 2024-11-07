package com.slackcat.app.modules.emojiText

import com.features.slackcat.models.SlackcatModule
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlin.random.Random

class EmojiTextModule : SlackcatModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val text = extractUserText(incomingChatMessage.userText)
        val outgoingText =
            if (text != null) {
                convertString(text)
            } else {"empty"}

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channeId,
                text = outgoingText,
            ),
        )
    }

    private fun extractUserText(userText: String): String? {
        val regex = """^\S+\s+(.*)""".toRegex()
        return regex.find(userText)?.groupValues?.get(1)
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

    override fun provideCommand(): String = "emojitext"
}
