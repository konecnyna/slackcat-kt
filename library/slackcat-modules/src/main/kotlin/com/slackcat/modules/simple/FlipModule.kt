package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule

class FlipModule : SlackcatModule() {
    private val flipMap =
        mapOf(
            // Lowercase letters
            'a' to 'ɐ', 'b' to 'q', 'c' to 'ɔ', 'd' to 'p', 'e' to 'ǝ', 'f' to 'ɟ',
            'g' to 'ƃ', 'h' to 'ɥ', 'i' to 'ᴉ', 'j' to 'ɾ', 'k' to 'ʞ', 'l' to 'l',
            'm' to 'ɯ', 'n' to 'u', 'o' to 'o', 'p' to 'd', 'q' to 'b', 'r' to 'ɹ',
            's' to 's', 't' to 'ʇ', 'u' to 'n', 'v' to 'ʌ', 'w' to 'ʍ', 'x' to 'x',
            'y' to 'ʎ', 'z' to 'z',
            // Uppercase letters
            'A' to '∀', 'B' to 'B', 'C' to 'Ɔ', 'D' to 'D', 'E' to 'Ǝ', 'F' to 'Ⅎ',
            'G' to 'פ', 'H' to 'H', 'I' to 'I', 'J' to 'ſ', 'K' to 'K', 'L' to '˥',
            'M' to 'W', 'N' to 'N', 'O' to 'O', 'P' to 'Ԁ', 'Q' to 'Q', 'R' to 'R',
            'S' to 'S', 'T' to '┴', 'U' to '∩', 'V' to 'Λ', 'W' to 'M', 'X' to 'X',
            'Y' to '⅄', 'Z' to 'Z',
            // Numbers
            '0' to '0', '1' to 'Ɩ', '2' to 'ᄅ', '3' to 'Ɛ', '4' to 'ㄣ', '5' to 'ϛ',
            '6' to '9', '7' to 'ㄥ', '8' to '8', '9' to '6',
            // Common punctuation
            ',' to '\'', '.' to '˙', '?' to '¿', '!' to '¡', '"' to ',', '\'' to ',',
            '`' to ',', '(' to ')', ')' to '(', '[' to ']', ']' to '[', '{' to '}',
            '}' to '{', '<' to '>', '>' to '<', '&' to '⅋', '_' to '‾',
        )

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        if (incomingChatMessage.userText.isEmpty()) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content = textMessage("Please provide text to flip. Example: ?flip hello world"),
                ),
            )
            return
        }

        val flippedText = flipText(incomingChatMessage.userText)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("(╯°□°）╯︵ ┻━ $flippedText ━┻"),
            ),
        )
    }

    private fun flipText(inputText: String): String {
        return inputText.reversed().map { char ->
            flipMap[char] ?: char
        }.joinToString("")
    }

    override fun commandInfo() = CommandInfo(command = "flip")

    override fun help(): BotMessage =
        buildMessage {
            heading("Flip Help")
            text("This module flips text upside down using Unicode characters.")
            text("Usage: ?flip [text]")
            text("Example: ?flip hello world → plɹoʍ ollǝɥ")
        }
}
