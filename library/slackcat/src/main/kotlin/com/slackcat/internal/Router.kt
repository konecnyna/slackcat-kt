package com.slackcat.internal

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Router(modules: List<SlackcatModule>) {
    private val featureCommandMap: MutableMap<String, SlackcatModule> = mutableMapOf()

    init {
        modules.map {
            try {
                featureCommandMap[it.provideCommand()] = it
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * true -> message was handled
     * false -> message was NOT handled
     */
    suspend fun onMessage(incomingMessage: IncomingChatMessage): Boolean {
        val command = CommandParser.extractCommand(incomingMessage.rawMessage)
        if (!CommandParser.validateCommandMessage(incomingMessage.rawMessage) || command == null) {
            return false
        }

        val feature = featureCommandMap[command] ?: return false
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
            false
        }
    }
}
