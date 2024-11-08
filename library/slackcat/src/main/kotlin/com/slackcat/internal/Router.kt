package com.slackcat.internal

import com.slackcat.models.SlackcatModule
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.common.CommandParser

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

    fun onMessage(message: IncomingChatMessage): Boolean {
        val command = CommandParser.extractCommand(message.rawMessage)
        if (!CommandParser.validateCommandMessage(message.rawMessage) || command == null) {
            return false
        }

        val feature = featureCommandMap[command] ?: return false
        feature.onInvoke(message)
        return true
    }

}
