package com.slackcat.modules.storage.kudos

import com.slackcat.chat.models.ChatClient
import com.slackcat.common.BotMessage
import com.slackcat.common.MessageStyle
import com.slackcat.common.buildMessage

class KudosLeaderboard(
    private val kudosDAO: KudosDAO,
    private val chatClient: ChatClient,
) {
    suspend fun getLeaderboardMessage(): BotMessage {
        val topKudos = kudosDAO.getTopKudos(10)

        if (topKudos.isEmpty()) {
            return buildMessage(MessageStyle.SUCCESS) {
                text("No kudos have been given yet! Be the first to spread some positivity with `?++ @username`")
            }
        }

        // Fetch all display names before building the message
        val userDisplayNames =
            topKudos.associate { kudosRow ->
                kudosRow.userId to chatClient.getUserDisplayName(kudosRow.userId).getOrElse { kudosRow.userId }
            }

        val leaderboardText =
            buildString {
                topKudos.forEachIndexed { index, kudosRow ->
                    val medal =
                        when (index) {
                            0 -> ":first_place_medal:"
                            1 -> ":second_place_medal:"
                            2 -> ":third_place_medal:"
                            else -> "${index + 1}."
                        }
                    val displayName = userDisplayNames[kudosRow.userId] ?: kudosRow.userId
                    val plusText = if (kudosRow.count == 1) "plus" else "pluses"
                    appendLine("$medal $displayName - ${kudosRow.count} $plusText")
                }
            }.trimEnd()

        return buildMessage(MessageStyle.SUCCESS) {
            heading(":trophy: Kudos Leaderboard :trophy:")
            divider()
            text(leaderboardText)
        }
    }
}
