package com.slackcat.app.modules.learn

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.models.UnhandledCommandPipe
import com.slackcat.presentation.buildMessage
import com.slackcat.app.modules.learn.LearnInsertRow as LearnInsertRow

class LearnModule : SlackcatModule(), StorageModule, UnhandledCommandPipe {
    private val learnDAO = LearnDAO()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val learnRequest = makeLearnRequest(incomingChatMessage)
            ?: return postHelpMessage(incomingChatMessage.channelId)

        val message =
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = "I got $learnRequest",
            )
        sendMessage(message)
    }

    override fun onUnhandledCommand(message: IncomingChatMessage) {
    }

    override fun provideCommand(): String = "learn"

    override fun help(): String =
        buildMessage {
            title("LearnModule Help")
            text("Create a custom command to recall text.")
            text("*Usage:* ?learn <key> <text>")
            text("You can then recall the text ?<key>")
        }

    override fun provideTable() = LearnDAO.LearnTable

    private fun makeLearnRequest(incomingChatMessage: IncomingChatMessage): LearnInsertRow? {
        if (incomingChatMessage.userText.isEmpty()) return null

        val regex = """^"?(\w+)"?\s+"?(.+?)"?$""".toRegex()
        val match = regex.matchEntire(incomingChatMessage.userText)

        return match?.let {
            val learnKey = it.groups[1]?.value
            val learnText = it.groups[2]?.value
            if (learnKey != null && learnText != null) {
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
}
