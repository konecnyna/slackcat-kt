package com.slackcat.models

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatConfig
import com.slackcat.presentation.text
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class SlackcatModule : KoinComponent {
    abstract suspend fun onInvoke(incomingChatMessage: IncomingChatMessage)

    abstract fun provideCommand(): String

    abstract fun help(): String

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
                message = text(help()),
            ),
        )
    }

    open fun aliases(): List<String> = emptyList()
}
