package com.slackcat.internal

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import com.slackcat.models.SlackcatModule

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

    fun onMessage(incomingMessage: IncomingChatMessage): Boolean {
        val command = CommandParser.extractCommand(incomingMessage.rawMessage)
        if (!CommandParser.validateCommandMessage(incomingMessage.rawMessage) || command == null) {
            return false
        }

        val feature = featureCommandMap[command] ?: return false
        when {
            incomingMessage.arguments.contains("--help") -> {
                val message =
                    OutgoingChatMessage(
                        channelId = incomingMessage.channelId,
                        text = feature.help(),
                    )
                feature.sendMessage(message = message)
            }
            else -> feature.onInvoke(incomingMessage)
        }
        return true
    }
}
