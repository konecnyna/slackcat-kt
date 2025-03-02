package com.slackcat.app.modules.emojitext


import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import emojiDictionary

class EmojiTextModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val input = parseInput(incomingChatMessage.userText ?: "")
        if (input == null) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    message = text(help()),
                )
            )
            return
        }

        val messageArray = mutableListOf<String>()
        val wrap = "--wrap" in incomingChatMessage.arguments
        val letterChunks = input.letterArray.chunked(if (wrap) 5 else 1000)

        for (chunk in letterChunks) {
            var line = ""
            chunk.forEach { letter ->
                val msg = getLetter(letter)
                line = mergeLines(line, msg)
            }

            line = line.replace("#", input.emojiOne)
            line = line.replace(".", input.emojiTwo)
            messageArray.add(line)
        }

        messageArray.forEach { message ->
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    message = text(message),
                )
            )
        }
    }

    private fun parseInput(userText: String): EmojiInput? {
        val emojiRegex = "(:[^:]*:)".toRegex()
        val emojis = emojiRegex.findAll(userText).map { it.value }.toList()
        if (emojis.isEmpty()) return null

        val textEmoji = emojis.getOrNull(0) ?: return null
        val bgEmoji = emojis.getOrNull(1) ?: ":transparent:"

        val cleanText = userText.replace(textEmoji, "").replace(bgEmoji, "").trim()
        val letterArray = cleanText.lowercase().toList().map { it.toString() }.toMutableList()

        letterArray.add(0, " ") // Padding
        letterArray.add(" ")

        return EmojiInput(textEmoji, bgEmoji, letterArray)
    }

    private fun getLetter(letter: String): String {
        return emojiDictionary[letter] ?: emojiDictionary["space"] ?: ""
    }

    private fun mergeLines(line: String, letter: String): String {
        if (line.isEmpty()) return letter
        val lineArray = line.split("\n").toMutableList()
        val letterArray = letter.split("\n")

        for (i in letterArray.indices) {
            if (i >= lineArray.size) {
                lineArray.add(letterArray[i])
            } else {
                lineArray[i] = lineArray[i].trimEnd() + "." + letterArray[i].trimStart()
            }
        }
        return lineArray.joinToString("\n")
    }

    override fun help(): String = buildMessage {
        title("EmojiText Help")
        text("Usage:?emoji-text <emoji_one> <emoji_two> text")
    }

    override fun provideCommand(): String = "emoji-text"
}

data class EmojiInput(
    val emojiOne: String,
    val emojiTwo: String,
    val letterArray: MutableList<String>
)
