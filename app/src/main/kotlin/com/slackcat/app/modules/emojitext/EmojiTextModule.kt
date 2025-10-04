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
                ),
            )
            return
        }

        val output = renderText(input.letterArray, input.emojiOne, input.emojiTwo)

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(output),
            ),
        )
    }

    private fun renderText(
        letters: List<String>,
        emojiOne: String,
        emojiTwo: String,
    ): String {
        // Remove leading/trailing whitespace from dictionary patterns to avoid extra spaces
        val cleanedDictionary =
            emojiDictionary.mapValues { (_, value) ->
                value.trim()
            }

        // Convert letters to their emoji representations and merge them
        var result = ""
        val letterDisplays =
            letters.map { letter ->
                getLetter(letter, cleanedDictionary)
            }

        // Initialize with first letter
        if (letterDisplays.isNotEmpty()) {
            result = letterDisplays[0]
        }

        // Merge remaining letters
        for (i in 1 until letterDisplays.size) {
            result = mergeLines(result, letterDisplays[i])
        }

        // Replace placeholders with actual emojis - avoid issues with whitespace
        return result.replace("#", emojiOne).replace(".", emojiTwo)
    }

    private fun parseInput(userText: String): EmojiInput? {
        // Match emoji pattern (text between colons)
        val emojiRegex = "(:[^:]*:)".toRegex()
        val emojis = emojiRegex.findAll(userText).map { it.value }.toList()

        if (emojis.isEmpty()) return null

        val textEmoji = emojis.getOrNull(0) ?: return null
        val bgEmoji = emojis.getOrNull(1) ?: ":transparent:"

        // Extract the text portion by removing emojis
        val cleanText = userText.replace(textEmoji, "").replace(bgEmoji, "").trim()

        // Convert text to character array
        val letterArray = cleanText.lowercase().toList().map { it.toString() }

        return EmojiInput(textEmoji, bgEmoji, letterArray)
    }

    private fun getLetter(
        letter: String,
        cleanedDictionary: Map<String, String>,
    ): String {
        // Get letter pattern from dictionary or default to space
        return cleanedDictionary[letter] ?: cleanedDictionary["space"] ?: ""
    }

    private fun mergeLines(
        line: String,
        letter: String,
    ): String {
        if (line.isEmpty()) return letter

        val lineArray = line.split("\n")
        val letterArray = letter.split("\n")

        // Make sure arrays are same size
        val maxSize = maxOf(lineArray.size, letterArray.size)
        val paddedLineArray = lineArray.padTo(maxSize, "")
        val paddedLetterArray = letterArray.padTo(maxSize, "")

        // Merge the lines horizontally without adding extra spaces
        return paddedLineArray.zip(paddedLetterArray) { a, b ->
            a + (if (a.isNotEmpty() && b.isNotEmpty()) "" else "") + b
        }.joinToString("\n")
    }

    // Extension function to pad a list to specified size
    private fun <T> List<T>.padTo(
        size: Int,
        padding: T,
    ): List<T> {
        return if (this.size >= size) {
            this
        } else {
            this + List(size - this.size) { padding }
        }
    }

    override fun help(): String =
        buildMessage {
            title("EmojiText Help")
            text("Convert text to emoji patterns")
            text("Usage: ?emoji-text :emoji_for_letters: [:emoji_for_background:] your text")
            text("Examples:")
            text("  ?emoji-text :fire: :black_large_square: Hello")
            text("  ?emoji-text :heart: Hello World")
        }

    override fun provideCommand(): String = "emoji-text"
}

data class EmojiInput(
    val emojiOne: String,
    val emojiTwo: String,
    val letterArray: List<String>,
)
