package com.slackcat.app.modules.jeopardy

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.RichTextMessage
import com.slackcat.presentation.buildRichMessage

class AliasHandler(private val jeopardyDAO: JeopardyDAO) {
    val alexTrebekish = "https://emoji.slack-edge.com/T07UUET6K51/alex-trebekish/5a325219e8701914.png"
    val alexTrebek = "https://emoji.slack-edge.com/T07UUET6K51/alex-trebek/e0c94c765b85bb71.jpg"
    val jeopardy = "https://emoji.slack-edge.com/T07UUET6K51/jeopardy/32e52d3ef5c5dc65.jpg"
    suspend fun handleAliases(
        incomingChatMessage: IncomingChatMessage
    ): OutgoingChatMessage? {
        val alias = JeopardyAliases.fromAlias(incomingChatMessage.command)
            ?: return null

        val message = when (alias) {
            JeopardyAliases.Answer -> handleAnswer(incomingChatMessage)
            JeopardyAliases.Points -> handlePoints(incomingChatMessage)
        }

        return OutgoingChatMessage(
            channelId = incomingChatMessage.channelId,
            message = message,
            botName = "Alex Trebek",
            botIcon = BotIcon.BotImageIcon(alexTrebek),
        )
        return null
    }

    private suspend fun handleAnswer(message: IncomingChatMessage): RichTextMessage {
        println("handling answer")
        val id = extractQuestionId(message.userText)?.toInt()
        val questionRow = id?.let { jeopardyDAO.getJeopardyQuestionById(it) }
        val questionPoints = questionRow?.value?.toInt()
        val userAnswer = extractAnswer(message.userText).toString()

        if (questionRow?.answer.toString().contains(userAnswer, true)) {
            if (questionPoints != null) {
                jeopardyDAO.updateUserScore(message.chatUser.toString(), questionPoints, true)
            }
            return jeopardize("CORRECT the answer is ${questionRow?.answer} for ${questionRow?.value}", jeopardy)

        } else {
            if (questionPoints != null) {
                jeopardyDAO.updateUserScore(message.chatUser.toString(), questionPoints, false)
            }
            return jeopardize("Wrong!!! What an idiot!: the answer is ${questionRow?.answer} for ${questionRow?.value}", alexTrebekish)
        }
    }

    private suspend fun handlePoints(message: IncomingChatMessage): RichTextMessage {
        val user = jeopardyDAO.getJeopardyScore(message.chatUser.toString())
        if (user == null) {
            return jeopardize("No score available", alexTrebekish)
        } else {
            return jeopardize(
                    "*<@${extractUserId(message.chatUser.toString())}> score:* \n " +
                    "*Points:* ${user.points} \n " +
                    "*Correct:* ${user.right} \n " +
                    "*Wrong:* ${user.wrong}", jeopardy
            )
        }
    }

    private fun jeopardize(message: String, imageUrl: String): RichTextMessage {
        return buildRichMessage {
            divider()
            section(
                text = message,
                imageUrl = imageUrl,
                altText = "alex quebec"
            )
            divider()
        }
    }

    private fun extractQuestionId(userText: String): String? {
        val regex = """(\d+)\s+(.*)""".toRegex()

        val matchResult = regex.find(userText)
        return matchResult?.groups?.get(1)?.value

    }

    private fun extractAnswer(userText: String): String? {
        val regex = """(\d+)\s+(.*)""".toRegex()

        val matchResult = regex.find(userText)
        return matchResult?.groups?.get(2)?.value
    }

    private fun extractUserId(user: String): String? {
        val regex = """userId=([A-Z0-9]+)""".toRegex()

        val matchResult = regex.find(user)
        return matchResult?.groups?.get(1)?.value
    }
}