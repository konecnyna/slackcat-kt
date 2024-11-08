package com.slackcat.internal

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Router(modules: List<SlackcatModule>) {
    private val featureCommandMap: Map<String, SlackcatModule> = buildMap {
        modules.forEach { module ->
            try {
                put(module.provideCommand(), module)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private val aliasCommandMap: Map<String, SlackcatModule> = buildMap {
        modules.forEach { module ->
            try {
                module.aliases().forEach { alias ->
                    put(alias, module)
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * true -> message was handled by module
     * false -> message was NOT handled module
     */
    suspend fun onMessage(incomingMessage: IncomingChatMessage): Boolean {
        val command = CommandParser.extractCommand(incomingMessage.rawMessage)
        if (!CommandParser.validateCommandMessage(incomingMessage.rawMessage) || command == null) {
            return false
        }

        val feature = featureCommandMap[command] // Primary commands take precedence
            ?: aliasCommandMap[command] // Check alias modules next
            ?: return false // Nothing to handle so bail.

        return try {
            when {
                incomingMessage.arguments.contains("--help") -> {
                    val helpMessage = OutgoingChatMessage(
                        channelId = incomingMessage.channelId,
                        text = feature.help()
                    )
                    feature.sendMessage(helpMessage)
                }
                else -> withContext(Dispatchers.IO) {
                    feature.onInvoke(incomingMessage)
                }
            }
            true
        } catch (exception: Exception) {
            handleError(feature, incomingMessage, exception)
            false
        }
    }

    private fun handleError(
        feature: SlackcatModule,
        incomingMessage: IncomingChatMessage,
        exception: Exception) {
        val errorMessage = buildMessage {
            title("🚨 Error")
            text("The ${feature::class.java.canonicalName} module encountered an error!")
            text("Error: '${exception.message}'")
        }
        feature.sendMessage(
            OutgoingChatMessage(
                channelId = incomingMessage.channelId,
                text = errorMessage
            )
        )
    }
}
