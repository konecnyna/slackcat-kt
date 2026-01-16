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
    protected open val spamProtectionEnabled: Boolean = true

    private val kudosDAO by lazy { KudosDAO(spamProtectionEnabled = spamProtectionEnabled) }
    private val leaderboard by lazy { KudosLeaderboard(kudosDAO, chatClient) }

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

        // Process all recipients and collect results
        val kudosResults =
            validIds.mapNotNull { recipientId ->
                // Check rate limit
                val rateLimitMessage =
                    kudosDAO.hasRecentKudos(
                        giverId = incomingChatMessage.chatUser.userId,
                        recipientId = recipientId,
                        threadTs = incomingChatMessage.messageId,
                    )

                if (rateLimitMessage != null) {
                    // Send DM for rate limit
                    sendMessage(
                        OutgoingChatMessage(
                            channelId = incomingChatMessage.chatUser.userId,
                            content = textMessage(rateLimitMessage),
                        ),
                    )
                    null // Skip this recipient
                } else {
                    // Give kudos
                    val kudos = kudosDAO.upsertKudos(recipientId)
                    kudosDAO.recordTransaction(
                        giverId = incomingChatMessage.chatUser.userId,
                        recipientId = recipientId,
                        threadTs = incomingChatMessage.messageId,
                    )
                    val displayName = chatClient.getUserDisplayName(recipientId).getOrThrow()
                    kudos to displayName
                }
            }

        // Send single aggregated message or individual message
        if (kudosResults.isNotEmpty()) {
            val messageText =
                if (kudosResults.size == 1) {
                    val (kudos, displayName) = kudosResults[0]
                    getKudosMessage(kudos, displayName)
                } else {
                    val updates =
                        kudosResults.joinToString(", ") { (kudos, displayName) ->
                            "$displayName (${kudos.count} ${if (kudos.count == 1) "plus" else "pluses"})"
                        }
                    "Kudos updated! $updates"
                }

            // Use time-window logic for message update/create
            val messageContent =
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    threadId = incomingChatMessage.messageId,
                    content = textMessage(messageText),
                )

            val activeMessage = kudosDAO.getActiveMessageForThread(incomingChatMessage.messageId)
            if (activeMessage != null) {
                updateMessage(
                    channelId = activeMessage.channelId,
                    messageTs = activeMessage.botMessageTs,
                    message = messageContent,
                )
            } else {
                sendMessage(messageContent).onSuccess { ts ->
                    kudosDAO.storeMessageWithWindow(
                        threadTs = incomingChatMessage.messageId,
                        botMessageTs = ts,
                        channelId = incomingChatMessage.channelId,
                    )
                }
            }
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
                println(
                    "[KudosModule] Reaction added: userId=${event.userId}, " +
                        "reaction=${event.reaction}, itemUserId=${event.itemUserId}",
                )
                // Give kudos to the user who authored the message
                event.itemUserId?.let { messageAuthorId ->
                    println("[KudosModule] Attempting to give kudos from ${event.userId} to $messageAuthorId")
                    if (isValidKudos(giverId = event.userId, recipientId = messageAuthorId)) {
                        println("[KudosModule] Kudos validation passed, calling giveKudosToUser")
                        try {
                            giveKudosToUser(
                                giverId = event.userId,
                                recipientId = messageAuthorId,
                                channelId = event.channelId,
                                threadId = event.messageTimestamp,
                            )
                            println("[KudosModule] giveKudosToUser completed successfully")
                        } catch (e: Exception) {
                            println("[KudosModule] ERROR in giveKudosToUser: ${e.message}")
                            e.printStackTrace()
                        }
                    } else {
                        println("[KudosModule] Kudos validation failed (self-kudos attempt)")
                    }
                } ?: run {
                    println("[KudosModule] itemUserId is null - cannot determine message author")
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
     * Gives kudos to a user triggered by a reaction.
     * Uses time-window logic for message aggregation.
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

        val displayName = chatClient.getUserDisplayName(recipientId).getOrThrow()
        val kudosMessage = getKudosMessage(updatedKudos, displayName)
        val messageContent =
            OutgoingChatMessage(
                channelId = channelId,
                threadId = threadId,
                content = textMessage(kudosMessage),
            )

        // Check for ACTIVE message within time window
        val activeMessage = kudosDAO.getActiveMessageForThread(threadId)

        if (activeMessage != null) {
            // Window still active - UPDATE existing message
            updateMessage(
                channelId = activeMessage.channelId,
                messageTs = activeMessage.botMessageTs,
                message = messageContent,
            )
        } else {
            // No active window - CREATE new message
            val result = sendMessage(messageContent)

            result.onSuccess { messageTs ->
                kudosDAO.storeMessageWithWindow(
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

    protected open fun getKudosMessage(
        kudos: KudosDAO.KudosRow,
        displayName: String,
    ): String {
        return when (kudos.count) {
            1 -> "$displayName now has ${kudos.count} plus"
            10 -> "$displayName now has ${kudos.count} pluses! Double digits!"
            69 -> "Nice $displayName"
            else -> {
                val plusText = if (kudos.count == 1) "plus" else "pluses"
                "$displayName now has ${kudos.count} $plusText"
            }
        }
    }
}
