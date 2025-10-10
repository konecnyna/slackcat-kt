package com.slackcat.modules.storage.kudos

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.database.DatabaseTable
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule

open class KudosModule : SlackcatModule(), StorageModule {
    private val kudosDAO = KudosDAO()
    private val leaderboard = KudosLeaderboard(kudosDAO)

    override fun tables(): List<DatabaseTable> = KudosDAO.getDatabaseTables()

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
                    content = textMessage("You'll go blind doing that!"),
                ),
            )
            return
        }

        validIds.forEach { recipientId ->
            giveKudosToUser(
                giverId = incomingChatMessage.chatUser.userId,
                recipientId = recipientId,
                channelId = incomingChatMessage.channelId,
                threadId = incomingChatMessage.messageId,
            )
        }
    }

    override fun commandInfo() =
        CommandInfo(
            command = "++",
            aliases = listOf("leaderboard", "kudosleaderboard", "pluses"),
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("KudosModule Help")
            text("Give kudos to your friends by using ?++ @username . See who can get the most!")
            text("You can also give kudos by reacting with :heavy_plus_sign: to their messages!")
            text("Use ?leaderboard to see the top 10 users with the most kudos!")
        }

    private suspend fun handleLeaderboard(incomingChatMessage: IncomingChatMessage) {
        val leaderboardMessage = leaderboard.getLeaderboardMessage()
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = leaderboardMessage,
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
                            giverId = event.userId,
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
     * Gives kudos to a user and sends/updates a confirmation message.
     * If a bot message already exists in this thread, it will be updated.
     * Otherwise, a new message will be sent and tracked.
     *
     * Includes rate limiting to prevent spam.
     */
    private suspend fun giveKudosToUser(
        giverId: String,
        recipientId: String,
        channelId: String,
        threadId: String,
    ) {
        // Check rate limits
        val rateLimitMessage =
            kudosDAO.hasRecentKudos(
                giverId = giverId,
                recipientId = recipientId,
                threadTs = threadId,
            )

        if (rateLimitMessage != null) {
            // Rate limited - send friendly denial message as DM to the giver
            // Send DM by using user ID as channel ID
            sendMessage(
                OutgoingChatMessage(
                    channelId = giverId,
                    content = textMessage(rateLimitMessage),
                ),
            )
            return
        }

        // Not rate limited - proceed with giving kudos
        val updatedKudos = kudosDAO.upsertKudos(recipientId)

        // Record this transaction for future rate limit checks
        kudosDAO.recordTransaction(
            giverId = giverId,
            recipientId = recipientId,
            threadTs = threadId,
        )

        val kudosMessage = getKudosMessage(updatedKudos)
        val messageContent =
            OutgoingChatMessage(
                channelId = channelId,
                threadId = threadId,
                content = textMessage(kudosMessage),
            )

        // Check if we already have a bot message in this thread
        val existingMessage = kudosDAO.getBotMessageForThread(threadId)

        if (existingMessage != null) {
            // Update the existing message
            updateMessage(
                channelId = existingMessage.channelId,
                messageTs = existingMessage.botMessageTs,
                message = messageContent,
            )
        } else {
            // Send a new message and store its timestamp
            val result = sendMessage(messageContent)

            result.onSuccess { messageTs ->
                kudosDAO.storeBotMessage(
                    threadTs = threadId,
                    botMessageTs = messageTs,
                    channelId = channelId,
                )
            }
        }
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
