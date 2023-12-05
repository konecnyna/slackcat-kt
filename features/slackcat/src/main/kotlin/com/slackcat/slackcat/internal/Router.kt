package com.slackcat.slackcat.internal

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.slackcat.models.SlackcatModule

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
        val command = extractCommand(message.rawMessage)
        if (!validateCommandMessage(message.rawMessage) || command == null) {
            return false
        }

        val feature = featureCommandMap[command] ?: return false
        feature.onInvoke(message)
        return true
    }

    private fun extractCommand(input: String): String? {
        val regex = "\\?(\\S+)".toRegex()
        val matchResult = regex.find(input)
        return matchResult?.groups?.get(1)?.value
    }

    private fun validateCommandMessage(message: String): Boolean {
        return when {
            message[0] != '?' -> false
            else -> true
        }
    }
}
