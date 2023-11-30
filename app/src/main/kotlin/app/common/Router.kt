package app.common

import data.chat.models.IncomingChatMessage
import features.FeatureGraph.featureModules
import features.common.FeatureModule


class Router {
    private val featureCommandMap: MutableMap<String, FeatureModule> = mutableMapOf()

    init {
        featureModules.map {
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