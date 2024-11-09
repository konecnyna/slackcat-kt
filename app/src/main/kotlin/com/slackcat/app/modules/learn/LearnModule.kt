package com.slackcat.app.modules.learn

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.models.UnhandledCommandModule
import com.slackcat.presentation.buildMessage

class LearnModule : SlackcatModule(), StorageModule, UnhandledCommandModule {
    private val learnFactory = LearnFactory()
    private val learnDAO = LearnDAO()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val learnRequest = learnFactory.makeLearnRequest(incomingChatMessage)
            ?: return postHelpMessage(incomingChatMessage.channelId)

        val message = when (learnDAO.insertLearn(learnRequest)) {
            true -> "I've learned ${learnRequest.learnKey} successfully. To recall use `?${learnRequest.learnKey}`"
            false -> "Failed to learn ${learnRequest.learnKey}. Please make sure command syntax is: ?learn \"<key>\" \"<text>'\""
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = message
            )
        )
    }

    override fun onUnhandledCommand(message: IncomingChatMessage): Boolean {
        val index = try {
            message.userText.toInt() - 1
        } catch (exception: NumberFormatException) {
            null
        }

        learnDAO.getLearn(key = message.command, index = index).fold({
            sendMessage(
                OutgoingChatMessage(
                    channelId = message.channelId,
                    text = it.learnText
                )
            )

            return true
        }, {
            return false
        })
    }

    override fun provideCommand(): String = "learn"

    override fun help(): String = buildMessage {
        title("LearnModule Help")
        text("Create a custom command to recall text.")
        text("*Usage:* ?learn \"<key>\" \"<text>'\"")
        text("You can then recall the text ?<key>")
    }

    override fun provideTable() = LearnDAO.LearnTable
}
