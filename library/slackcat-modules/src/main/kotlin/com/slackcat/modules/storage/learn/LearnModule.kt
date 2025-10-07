package com.slackcat.modules.storage.learn

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.models.UnhandledCommandModule
import com.slackcat.presentation.buildRichMessage
import com.slackcat.presentation.text
import org.jetbrains.exposed.sql.Table

class LearnModule : SlackcatModule(), StorageModule, UnhandledCommandModule {
    private val learnFactory = LearnFactory()
    private val learnDAO = LearnDAO()
    private val aliasHandler = LearnAliasHandler(learnDAO)

    override fun tables(): List<Table> = listOf(LearnDAO.LearnTable)

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val aliasMessage = aliasHandler.handleAliases(incomingChatMessage)
        if (aliasMessage != null) {
            sendMessage(aliasMessage)
            return
        }

        println(incomingChatMessage.userText)

        val learnRequest = learnFactory.makeLearnRequest(incomingChatMessage)
        if (learnRequest == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val message =
            when (learnDAO.insertLearn(learnRequest)) {
                true ->
                    "I've learned ${learnRequest.learnKey} successfully. " +
                        "To recall use `?${learnRequest.learnKey}`"
                false ->
                    "Failed to learn ${learnRequest.learnKey}. " +
                        "Please make sure command syntax is: ?learn \"<key>\" \"<text>'\""
            }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(message),
            ),
        )
    }

    override suspend fun onUnhandledCommand(message: IncomingChatMessage): Boolean {
        val index =
            try {
                message.userText.toInt() - 1
            } catch (exception: NumberFormatException) {
                null
            }

        learnDAO.getLearn(key = message.command, index = index).fold(
            onSuccess = {
                sendLearnMessage(channelId = message.channelId, learnItem = it)
                return true
            },
            onFailure = {
                return false
            },
        )
    }

    private suspend fun sendLearnMessage(
        channelId: String,
        learnItem: LearnDAO.LearnRow,
    ) {
        val text = learnItem.learnText.replace("<", "").replace(">", "")
        val isImage = text.matches(Regex("https?://.*\\.(jpg|jpeg|png|gif|bmp|svg)$"))
        when (isImage) {
            true -> {
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        message =
                            buildRichMessage {
                                image(
                                    imageUrl = text,
                                    altText = "learn image",
                                )
                            },
                    ),
                )
            }

            false -> {
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        message = text(learnItem.learnText),
                    ),
                )
            }
        }
    }

    override fun help(): BotMessage =
        buildMessage {
            heading("LearnModule Help")
            text("Create a custom command to recall text.")
            text("*Usage:* ?learn \"<key>\" \"<text>'\"")
            text("You can then recall the text ?<key>")
        }

    override fun provideCommand(): String = "learn"

    override fun aliases(): List<String> = LearnAliases.entries.map { it.alias }
}

enum class LearnAliases(val alias: String) {
    Unlearn("unlearn"),
    List("list"),
    ;

    companion object {
        private val aliasMap = entries.associateBy { it.alias }

        fun fromAlias(alias: String): LearnAliases? {
            return aliasMap[alias.lowercase()]
        }
    }
}
