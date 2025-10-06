package com.slackcat.modules.storage.kudos

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import org.jetbrains.exposed.sql.Table

open class KudosModule : SlackcatModule(), StorageModule {
    private val kudosDAO = KudosDAO()
    private val leaderboard = KudosLeaderboard(kudosDAO)

    override fun tables(): List<Table> = listOf(KudosDAO.KudosTable)

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // Check if this is a leaderboard command
        if (incomingChatMessage.command == "leaderboard" ||
            incomingChatMessage.command == "kudosleaderboard" ||
            incomingChatMessage.command == "pluses"
        ) {
            handleLeaderboard(incomingChatMessage)
            return
        }

        // Handle kudos giving
        val allIds = extractUserIds(incomingChatMessage.userText)
        val validIds = filterValidKudosRecipients(allIds, incomingChatMessage.chatUser.userId)

        if (allIds.size == 1 && validIds.isEmpty()) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    threadId = incomingChatMessage.messageId,
                    message = text("You'll go blind doing that!"),
                ),
            )
            return
        }

        validIds.forEach { recipientId ->
            giveKudosToUser(
                recipientId = recipientId,
                channelId = incomingChatMessage.channelId,
                threadId = incomingChatMessage.messageId,
            )
        }
    }

    override fun provideCommand(): String = "++"

    override fun aliases(): List<String> = listOf("leaderboard", "kudosleaderboard", "pluses")

    override fun help(): String =
        buildMessage {
            title("KudosModule Help")
            text("Give kudos to your friends by using ?++ @username . See who can get the most!")
            text("You can also give kudos by reacting with :heavy_plus_sign: to their messages!")
            text("Use ?leaderboard to see the top 10 users with the most kudos!")
        }

    private suspend fun handleLeaderboard(incomingChatMessage: IncomingChatMessage) {
        val leaderboardText = leaderboard.getLeaderboardMessage()
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message =
                    com.slackcat.presentation.messageWithAttachment("#2eb886") {
                        section(leaderboardText)
                    },
            ),
        )
    }

    override fun reactionsToHandle(): Set<String> = setOf("heavy_plus_sign")

    override suspend fun onReaction(event: SlackcatEvent) {
        when (event) {
            is SlackcatEvent.ReactionAdded -> {
                // Give kudos to the user who authored the message
                event.itemUserId?.let { messageAuthorId ->
                    if (isValidKudos(giverId = event.userId, recipientId = messageAuthorId)) {
                        giveKudosToUser(
                            recipientId = messageAuthorId,
                            channelId = event.channelId,
                            threadId = event.messageTimestamp,
                        )
                    }
                }
            }

            is SlackcatEvent.ReactionRemoved -> {
                // Optionally handle kudos removal
                // For now, we'll keep kudos even if reactions are removed
            }

            else -> {
                // Ignore other events
            }
        }
    }

    /**
     * Centralized validation logic for kudos.
     * Returns true if the kudos is valid (not self-kudos).
     */
    private fun isValidKudos(
        giverId: String,
        recipientId: String,
    ): Boolean {
        return giverId != recipientId
    }

    /**
     * Filters a collection of user IDs to remove the giver (no self-kudos).
     */
    private fun filterValidKudosRecipients(
        recipientIds: Collection<String>,
        giverId: String,
    ): List<String> {
        return recipientIds.filter { isValidKudos(giverId, it) }
    }

    /**
     * Gives kudos to a user and sends a confirmation message.
     */
    private suspend fun giveKudosToUser(
        recipientId: String,
        channelId: String,
        threadId: String,
    ) {
        val updatedKudos = kudosDAO.upsertKudos(recipientId)
        sendMessage(
            OutgoingChatMessage(
                channelId = channelId,
                threadId = threadId,
                message = text(getKudosMessage(updatedKudos)),
            ),
        )
    }

    private fun extractUserIds(userText: String): Set<String> {
        val pattern = """<@(\w+)>""".toRegex()
        return pattern.findAll(userText).map { it.groupValues[1] }.toSet()
    }

    protected open fun getKudosMessage(kudos: KudosDAO.KudosRow): String {
        return when (kudos.count) {
            1 -> "<@${kudos.userId}> now has ${kudos.count} plus"
            10 -> "<@${kudos.userId}> now has ${kudos.count} pluses! Double digits!"
            69 -> "Nice <@${kudos.userId}>"
            else -> {
                val plusText = if (kudos.count == 1) "plus" else "pluses"
                "<@${kudos.userId}> now has ${kudos.count} $plusText"
            }
        }
    }
}
