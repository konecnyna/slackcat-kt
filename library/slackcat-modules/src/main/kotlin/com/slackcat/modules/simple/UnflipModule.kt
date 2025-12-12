package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule

class UnflipModule : SlackcatModule() {
    private val unflipMap =
        mapOf(
            // Lowercase letters (reversed)
            'ɐ' to 'a', 'q' to 'b', 'ɔ' to 'c', 'p' to 'd', 'ǝ' to 'e', 'ɟ' to 'f',
            'ƃ' to 'g', 'ɥ' to 'h', 'ᴉ' to 'i', 'ɾ' to 'j', 'ʞ' to 'k', 'l' to 'l',
            'ɯ' to 'm', 'u' to 'n', 'o' to 'o', 'd' to 'p', 'b' to 'q', 'ɹ' to 'r',
            's' to 's', 'ʇ' to 't', 'n' to 'u', 'ʌ' to 'v', 'ʍ' to 'w', 'x' to 'x',
            'ʎ' to 'y', 'z' to 'z',
            // Uppercase letters (reversed)
            '∀' to 'A', 'B' to 'B', 'Ɔ' to 'C', 'D' to 'D', 'Ǝ' to 'E', 'Ⅎ' to 'F',
            'פ' to 'G', 'H' to 'H', 'I' to 'I', 'ſ' to 'J', 'K' to 'K', '˥' to 'L',
            'W' to 'M', 'N' to 'N', 'O' to 'O', 'Ԁ' to 'P', 'Q' to 'Q', 'R' to 'R',
            'S' to 'S', '┴' to 'T', '∩' to 'U', 'Λ' to 'V', 'M' to 'W', 'X' to 'X',
            '⅄' to 'Y', 'Z' to 'Z',
            // Numbers (reversed)
            '0' to '0', 'Ɩ' to '1', 'ᄅ' to '2', 'Ɛ' to '3', 'ㄣ' to '4', 'ϛ' to '5',
            '9' to '6', 'ㄥ' to '7', '8' to '8', '6' to '9',
            // Common punctuation (reversed)
            '\'' to ',', '˙' to '.', '¿' to '?', '¡' to '!', ',' to '"',
            ')' to '(', '(' to ')', ']' to '[', '[' to ']', '}' to '{',
            '{' to '}', '>' to '<', '<' to '>', '⅋' to '&', '‾' to '_',
        )

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        if (incomingChatMessage.userText.isEmpty()) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content = textMessage("Please provide flipped text to unflip. Example: ?unflip plɹoʍ ollǝɥ"),
                ),
            )
            return
        }

        val unflippedText = unflipText(incomingChatMessage.userText)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("┬─┬ ノ( ゜-゜ノ) $unflippedText"),
            ),
        )
    }

    private fun unflipText(inputText: String): String {
        return inputText.map { char ->
            unflipMap[char] ?: char
        }.reversed().joinToString("")
    }

    override fun commandInfo() = CommandInfo(command = "unflip")

    override fun help(): BotMessage =
        buildMessage {
            heading("Unflip Help")
            text("This module converts flipped text back to normal.")
            text("Usage: ?unflip [flipped text]")
            text("Example: ?unflip plɹoʍ ollǝɥ → hello world")
        }
}
