package com.slackcat.app.modules.learn

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.models.UnhandledCommandModule
import com.slackcat.presentation.RichMessage
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Alias

class LearnModule : SlackcatModule(), StorageModule, UnhandledCommandModule {
    private val learnFactory = LearnFactory()
    private val learnDAO = LearnDAO()
    private val aliasHandler = AliasHandler(learnDAO)

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val aliasMessage = aliasHandler.handleAliases(incomingChatMessage)
        if (aliasMessage != null) {
            sendMessage(aliasMessage)
            return
        }


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
            sendLearnMessage(channelId = message.channelId, learnItem = it)
            return true
        }, {
            return false
        })
    }

    private fun sendLearnMessage(
        channelId: String,
        learnItem: LearnDAO.LearnRow
    ) {
        val text = learnItem.learnText.replace("<", "").replace(">", "")
        val isImage = text.matches(Regex("https?://.*\\.(jpg|jpeg|png|gif|bmp|svg)$"))
        when (isImage) {
            true -> {
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        richText = buildRichMessage {
                            image(
                                imageUrl = text,
                                altText = "learn image"
                            )
                        }
                    )
                )
            }

            false -> {
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        text = learnItem.learnText
                    )
                )
            }
        }

    }


    override fun help(): String = buildMessage {
        title("LearnModule Help")
        text("Create a custom command to recall text.")
        text("*Usage:* ?learn \"<key>\" \"<text>'\"")
        text("You can then recall the text ?<key>")
    }

    override fun provideCommand(): String = "learn"
    override fun provideTable() = LearnDAO.LearnTable
    override fun aliases(): List<String> = LearnAliases.entries.map { it.alias }

}


enum class LearnAliases(val alias: String) {
    Unlearn("unlearn"),
    List("list");

    companion object {
        private val aliasMap = entries.associateBy { it.alias }
        fun fromAlias(alias: String): LearnAliases? {
            return aliasMap[alias.lowercase()]
        }
    }
}