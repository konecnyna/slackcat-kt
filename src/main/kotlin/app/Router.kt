package app

import features.ChatModule
import features.FeatureEntry.features
import java.time.Instant.now
import kotlin.reflect.full.createInstance


class Router(private val chatClient: ChatClient) {
    private val featureCommandMap: MutableMap<String, ChatModule> = mutableMapOf()

    init {
        features.map { kClass ->
            try {
                val featureClass: ChatModule = kClass.createInstance()
                featureClass.chatClient = chatClient
                featureCommandMap[featureClass.provideCommand()] = featureClass
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
                id = now().toString(),
                rawMessage = message,
                userText = message.replace("?${feature.provideCommand()}", "").trim()
            )
        )
        return true
    }

    fun validateCommandMessage(message: String): Boolean {
        return when {
            message[0] != '?' -> false
            else -> true
        }
    }

    data class Message(
        val id: String,
        val rawMessage: String,
        val userText: String,
    )
}