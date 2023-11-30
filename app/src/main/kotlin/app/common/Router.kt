package app.common

import app.models.ChatUser
import app.models.Message
import features.FeatureGraph.featureModules
import features.common.FeatureModule
import java.time.Instant.now


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
    fun onMessage(message: String): Boolean {
        if (!validateCommandMessage(message)) {
            return false
        }

        // Drop ? arg
        val feature = featureCommandMap[message.drop(1)] ?: return false

        feature.onInvoke(
            Message(
                chatUser = ChatUser(userId = "42069"),
                messageId = now().toString(),
                rawMessage = message,
                userText = message
                    .lowercase()
                    .replace("?${feature.provideCommand()}", "")
                    .trim()
            )
        )
        return true
    }

    private fun validateCommandMessage(message: String): Boolean {
        return when {
            message[0] != '?' -> false
            else -> true
        }
    }
}