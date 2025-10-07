package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.common.buildMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.text
import java.util.concurrent.ConcurrentHashMap

/**
 * Example module that demonstrates reaction event handling.
 * This module tracks how many times specific reactions have been added across all messages.
 */
class ReactionCounterModule : SlackcatModule() {
    // Thread-safe map to store reaction counts
    private val reactionCounts = ConcurrentHashMap<String, Int>()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // Display current reaction counts
        val message =
            if (reactionCounts.isEmpty()) {
                "No reactions tracked yet! Try adding :thumbsup:, :heart:, or :fire: reactions to messages."
            } else {
                val countsList =
                    reactionCounts
                        .entries
                        .sortedByDescending { it.value }
                        .joinToString("\n") { ":${it.key}: - ${it.value} time${if (it.value == 1) "" else "s"}" }
                "Reaction counts:\n$countsList"
            }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(message),
            ),
        )
    }

    override fun provideCommand(): String = "reactions"

    override fun help(): BotMessage =
        buildMessage {
            heading("Reaction Counter Help")
            text("This module tracks reactions added to messages.")
            text("Usage: ?reactions - Display current reaction counts")
            text("Tracked reactions: :thumbsup:, :heart:, :fire:, :+1:")
        }

    override fun reactionsToHandle(): Set<String> =
        setOf(
            "thumbsup",
            "+1",
            "heart",
            "fire",
        )

    override suspend fun onReaction(event: SlackcatEvent) {
        when (event) {
            is SlackcatEvent.ReactionAdded -> {
                // Increment the count for this reaction
                reactionCounts.compute(event.reaction) { _, count ->
                    (count ?: 0) + 1
                }
            }

            is SlackcatEvent.ReactionRemoved -> {
                // Decrement the count for this reaction
                reactionCounts.compute(event.reaction) { _, count ->
                    val newCount = (count ?: 0) - 1
                    if (newCount <= 0) null else newCount // Remove if count reaches 0
                }
            }

            else -> {
                // Ignore other events
            }
        }
    }
}
