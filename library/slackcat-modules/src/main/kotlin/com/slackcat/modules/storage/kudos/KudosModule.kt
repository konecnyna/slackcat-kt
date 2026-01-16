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

        // Determine the effective thread root (for top-level messages, use messageId)
        val threadRoot = incomingChatMessage.threadId ?: incomingChatMessage.messageId
        println(
            "[KudosModule] Processing ?++ command: " +
                "threadId=${incomingChatMessage.threadId}, " +
                "messageId=${incomingChatMessage.messageId}, " +
                "threadRoot=$threadRoot",
        )

        // Handle kudos giving
        val allIds = extractUserIds(incomingChatMessage.userText)
        val validIds = filterValidKudosRecipients(allIds, incomingChatMessage.chatUser.userId)
        println("[KudosModule] Extracted IDs: allIds=$allIds, validIds=$validIds")

        if (allIds.size == 1 && validIds.isEmpty()) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    threadId = threadRoot,
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
                        threadTs = threadRoot,
                    )

                println("[KudosModule] Rate limit check: recipientId=$recipientId, rateLimitMessage=$rateLimitMessage")

                if (rateLimitMessage != null) {
                    // Send DM for rate limit
                    println("[KudosModule] Rate limited - sending DM")
                    sendMessage(
                        OutgoingChatMessage(
                            channelId = incomingChatMessage.chatUser.userId,
                            content = textMessage(rateLimitMessage),
                        ),
                    )
                    null // Skip this recipient
                } else {
                    // Give kudos
                    println("[KudosModule] Giving kudos to $recipientId")
                    val kudos = kudosDAO.upsertKudos(recipientId)
                    kudosDAO.recordTransaction(
                        giverId = incomingChatMessage.chatUser.userId,
                        recipientId = recipientId,
                        threadTs = threadRoot,
                    )
                    val displayName = chatClient.getUserDisplayName(recipientId).getOrThrow()
                    println("[KudosModule] Kudos given: ${kudos.count} pluses to $displayName")
                    kudos to displayName
                }
            }

        // Send aggregated message with deltas
        if (kudosResults.isNotEmpty()) {
            println("[KudosModule] kudosResults not empty, preparing message")

            // Build current user counts from this invocation
            val currentUserCounts =
                kudosResults.associate { (kudos, _) ->
                    kudos.userId to kudos.count
                }

            val activeMessage = kudosDAO.getActiveMessageForThread(threadRoot)
            println("[KudosModule] Active message for thread $threadRoot: $activeMessage")

            if (activeMessage != null) {
                println("[KudosModule] Updating existing message: ${activeMessage.botMessageTs}")
                // Merge previous and current counts
                val previousUserCounts = activeMessage.userCounts
                val allUserIds = (previousUserCounts.keys + currentUserCounts.keys).toSet()

                // Build aggregated message with deltas
                val userMessages =
                    allUserIds.mapNotNull { userId ->
                        val currentCount = currentUserCounts[userId]
                        val previousCount = previousUserCounts[userId]

                        // Get display name (from kudosResults or fetch if needed)
                        val displayName =
                            kudosResults.find { (kudos, _) -> kudos.userId == userId }?.second
                                ?: chatClient.getUserDisplayName(userId).getOrNull() ?: userId

                        when {
                            currentCount != null && previousCount != null -> {
                                // User was updated
                                val delta = currentCount - previousCount
                                "$displayName: ${currentCount} ${pluralize(currentCount)} (+$delta more)"
                            }
                            currentCount != null -> {
                                // New user added
                                "$displayName now has ${currentCount} ${pluralize(currentCount)}"
                            }
                            else -> {
                                // User was in previous but not current (shouldn't happen)
                                null
                            }
                        }
                    }

                val messageText = userMessages.joinToString(" | ")
                val newUserCounts = previousUserCounts + currentUserCounts

                updateMessage(
                    channelId = activeMessage.channelId,
                    messageTs = activeMessage.botMessageTs,
                    message =
                        OutgoingChatMessage(
                            channelId = incomingChatMessage.channelId,
                            threadId = threadRoot,
                            content = textMessage(messageText),
                        ),
                )

                // Update stored user counts
                kudosDAO.updateMessageUserCounts(
                    threadTs = threadRoot,
                    botMessageTs = activeMessage.botMessageTs,
                    userCounts = newUserCounts,
                )
            } else {
                println("[KudosModule] Creating new message")
                // First time - no deltas needed
                val userMessages =
                    kudosResults.map { (kudos, displayName) ->
                        "$displayName now has ${kudos.count} ${pluralize(kudos.count)}"
                    }

                val messageText = userMessages.joinToString(" | ")

                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        threadId = threadRoot,
                        content = textMessage(messageText),
                    ),
                ).onSuccess { ts ->
                    println("[KudosModule] Message sent successfully, storing with window: $ts")
                    kudosDAO.storeMessageWithWindow(
                        threadTs = threadRoot,
                        botMessageTs = ts,
                        channelId = incomingChatMessage.channelId,
                        userCounts = currentUserCounts,
                    )
                }
            }
        } else {
            println("[KudosModule] kudosResults is empty - no message will be sent")
        }
    }

    private fun pluralize(count: Int): String {
        return if (count == 1) "plus" else "pluses"
    }

    override fun commandInfo() =
        CommandInfo(
            command = "++",
            aliases = listOf("kudos", "leaderboard", "kudosleaderboard", "pluses"),
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
                            // Use thread root for proper message aggregation
                            val threadRoot = event.threadTimestamp ?: event.messageTimestamp
                            giveKudosToUser(
                                giverId = event.userId,
                                recipientId = messageAuthorId,
                                channelId = event.channelId,
                                threadId = threadRoot,
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
        val currentUserCounts = mapOf(recipientId to updatedKudos.count)

        // Check for ACTIVE message within time window
        val activeMessage = kudosDAO.getActiveMessageForThread(threadId)

        if (activeMessage != null) {
            // Window still active - UPDATE existing message with aggregation and deltas
            val previousUserCounts = activeMessage.userCounts
            val allUserIds = (previousUserCounts.keys + currentUserCounts.keys).toSet()

            // Build aggregated message with deltas
            val userMessages =
                allUserIds.mapNotNull { userId ->
                    val currentCount = currentUserCounts[userId]
                    val previousCount = previousUserCounts[userId]

                    // Get display name
                    val userName =
                        if (userId == recipientId) {
                            displayName
                        } else {
                            chatClient.getUserDisplayName(userId).getOrNull() ?: userId
                        }

                    when {
                        currentCount != null && previousCount != null -> {
                            // User was updated
                            val delta = currentCount - previousCount
                            "$userName: ${currentCount} ${pluralize(currentCount)} (+$delta more)"
                        }
                        currentCount != null -> {
                            // New user added
                            "$userName now has ${currentCount} ${pluralize(currentCount)}"
                        }
                        previousCount != null -> {
                            // User was in previous but not current - keep them in the display
                            "$userName: ${previousCount} ${pluralize(previousCount)}"
                        }
                        else -> null
                    }
                }

            val messageText = userMessages.joinToString(" | ")
            val newUserCounts = previousUserCounts + currentUserCounts

            updateMessage(
                channelId = activeMessage.channelId,
                messageTs = activeMessage.botMessageTs,
                message =
                    OutgoingChatMessage(
                        channelId = channelId,
                        threadId = threadId,
                        content = textMessage(messageText),
                    ),
            )

            // Update stored user counts
            kudosDAO.updateMessageUserCounts(
                threadTs = threadId,
                botMessageTs = activeMessage.botMessageTs,
                userCounts = newUserCounts,
            )
        } else {
            // No active window - CREATE new message
            val messageText = "$displayName now has ${updatedKudos.count} ${pluralize(updatedKudos.count)}"

            val result =
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        threadId = threadId,
                        content = textMessage(messageText),
                    ),
                )

            result.onSuccess { messageTs ->
                kudosDAO.storeMessageWithWindow(
                    threadTs = threadId,
                    botMessageTs = messageTs,
                    channelId = channelId,
                    userCounts = currentUserCounts,
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
