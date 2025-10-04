package com.slackcat.app.modules.jeopardy

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.RichTextMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage

class JeopardyModule(
    private val networkClient: NetworkClient,
) : SlackcatModule(), StorageModule {
    private val jeopardyDAO by lazy { JeopardyDAO(networkClient) }
    private val aliasHandler by lazy { JeopardyAliasHandler(jeopardyDAO) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        if (jeopardyDAO.getJeopardyTableLength() == 0L) {
            jeopardyDAO.hydrateJeopardyQuestions()
        }
        val aliasMessage = aliasHandler.handleAliases(incomingChatMessage)
        if (aliasMessage != null) {
            sendMessage(aliasMessage)
            return
        }
        val value = incomingChatMessage.userText
        val questionRow = jeopardyDAO.getJeopardyQuestion(value)
        val message = buildJeopardyMessage(questionRow)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = message,
                botName = "Alex Trebek",
                botIcon =
                    BotIcon.BotImageIcon(
                        "https://emoji.slack-edge.com/T07UUET6K51/alex-trebek/e0c94c765b85bb71.jpg",
                    ),
            ),
        )
    }

    private fun buildJeopardyMessage(question: JeopardyDAO.JeopardyQuestionRow): RichTextMessage {
        return buildRichMessage {
            divider()
            section(
                text =
                    "*The category is '${question.category}' for $${question.value}:* \n " +
                        "${question.question} \n Question ID: ${question.id}",
                imageUrl = "https://emoji.slack-edge.com/T07UUET6K51/jeopardy/32e52d3ef5c5dc65.jpg",
                altText = "alex quebec",
            )
            divider()
        }
    }

    override fun provideCommand(): String = "jeopardy"

    override fun help(): String =
        buildMessage {
            title("JeopardyModule Help")
            text(
                "Get a question using ?jeopardy <value> (ex ?jeopardy 300) \n" +
                    "Answer a question using ?jeopardy-answer <questionId> <your answer> \n" +
                    "Check your current points with ?jeopardy-points",
            )
        }

    override fun provideTables() = listOf(JeopardyDAO.JeopardyQuestionsTable, JeopardyDAO.JeopardyScoreTable)

    override fun aliases(): List<String> = JeopardyAliases.entries.map { it.alias }
}

enum class JeopardyAliases(val alias: String) {
    Answer("jeopardy-answer"),
    Points("jeopardy-points"),
    ;

    companion object {
        private val aliasMap = entries.associateBy { it.alias }

        fun fromAlias(alias: String): JeopardyAliases? {
            return aliasMap[alias.lowercase()]
        }
    }
}
