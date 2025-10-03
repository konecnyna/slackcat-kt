package com.slackcat.modules.storage.learn

import com.slackcat.chat.models.IncomingChatMessage

class LearnFactory {
    fun makeLearnRequest(incomingChatMessage: IncomingChatMessage): LearnInsertRow? {
        if (incomingChatMessage.userText.isEmpty()) return null
        val userText = incomingChatMessage.userText
        return parseQuotedFormat(userText, incomingChatMessage)
            ?: parseUnquotedFormat(userText, incomingChatMessage)
    }

    // Parses the quoted format: ?learn "key" "value"
    private fun parseQuotedFormat(
        text: String,
        incomingChatMessage: IncomingChatMessage
    ): LearnInsertRow? {
        val regex = """^\?learn\s+"(\w+)"\s+"((?s).+)"$""".toRegex()
        val match = regex.matchEntire(text)

        return match?.let {
            val learnKey = it.groups[1]?.value
            val learnText = it.groups[2]?.value
            createLearnInsertRow(learnKey, learnText, incomingChatMessage)
        }
    }

    // Parses the unquoted format: ?learn key rest of the text
    private fun parseUnquotedFormat(
        text: String,
        incomingChatMessage: IncomingChatMessage
    ): LearnInsertRow? {
        // Updated regex to handle newlines and whitespace properly
        val regex = """^(\S+)\s+((?s).+)${'$'}""".toRegex() // Allows matching multi-line text
        val match = regex.matchEntire(text)

        return match?.let {
            val learnKey = it.groups[1]?.value
            val learnText = it.groups[2]?.value
            createLearnInsertRow(learnKey, learnText, incomingChatMessage)
        }
    }

    // Creates a LearnInsertRow object if both learnKey and learnText are valid
    private fun createLearnInsertRow(
        learnKey: String?,
        learnText: String?,
        incomingChatMessage: IncomingChatMessage
    ): LearnInsertRow? {
        return if (learnKey != null && learnText != null) {
            LearnInsertRow(
                learnedBy = incomingChatMessage.chatUser.userId,
                learnKey = learnKey,
                learnText = learnText,
            )
        } else {
            null
        }
    }
}