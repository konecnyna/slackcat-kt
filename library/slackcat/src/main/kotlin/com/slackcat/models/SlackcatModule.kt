package com.slackcat.models

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.SlackcatConfig
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class SlackcatModule : KoinComponent {
    abstract suspend fun onInvoke(incomingChatMessage: IncomingChatMessage)

    open fun commandInfo(): CommandInfo {
        throw NotImplementedError("Module must implement commandInfo()")
    }

    abstract fun help(): BotMessage

    val chatClient: ChatClient by inject()

    val coroutineScope: CoroutineScope by inject()

    val config: SlackcatConfig by inject()

    // Modules can override these to customize their bot name/icon
    open val botName: String? = null
    open val botIcon: BotIcon? = null

    suspend fun sendMessage(message: OutgoingChatMessage): Result<Unit> {
        // Apply module-level overrides or fall back to config providers
        val finalBotName = botName ?: config.botNameProvider()
        val finalBotIcon = botIcon ?: config.botIconProvider()

        return chatClient.sendMessage(message, finalBotName, finalBotIcon)
    }

    suspend fun postHelpMessage(channelId: String): Result<Unit> {
        return sendMessage(
            OutgoingChatMessage(
                channelId = channelId,
                content = help(),
            ),
        )
    }

    /**
     * Override this method to specify which reaction emojis this module should handle.
     * Return a set of emoji names (without colons) that this module is interested in.
     * Return an empty set (default) to not handle any reactions.
     *
     * Example: setOf("thumbsup", "heart", "fire", "+1")
     */
    open fun reactionsToHandle(): Set<String> = emptySet()

    /**
     * Override this method to handle reaction events.
     * This will only be called if reactionsToHandle() returns a non-empty set
     * and the reaction matches one of the emojis in that set.
     *
     * @param event The reaction event (either ReactionAdded or ReactionRemoved)
     */
    open suspend fun onReaction(event: com.slackcat.common.SlackcatEvent) {
        // Default implementation does nothing
    }
}
