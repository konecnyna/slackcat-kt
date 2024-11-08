package com.slackcat.app.modules.translate

import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class TranslateModule : SlackcatModule() {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        globalScope.launch {
            val userText = extractUserText(incomingChatMessage.userText)
            val translationType = extractUserTranslateType(incomingChatMessage.userText)
            val response =
                if (translationType != null && userText != null) {
                    post(userText, translationType)
                } else {
                    null
                }

            val outgoingText =
                when (response) {
                    is ErrorResponse -> response.error.message
                    is SuccessResponse -> response.contents.translated
                    null -> "Translate failed: sorry bub"
                }

            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    text = outgoingText,
                ),
            )
        }
    }

    private suspend fun post(
        text: String,
        translationType: String,
    ): ApiResponse {
        val response =
            slackcatNetworkClient.post(
                "https://api.funtranslations.com/translate/$translationType",
                """{"text":"$text"}""",
            )
        return deserialize(response)
    }

    private fun deserialize(response: String): ApiResponse {
        return when (parseApiResponse(response)) {
            is SuccessResponse ->
                json.decodeFromString(
                    SuccessResponse.serializer(),
                    response,
                )

            is ErrorResponse -> json.decodeFromString(ErrorResponse.serializer(), response)
        }
    }

    private fun extractUserText(userText: String): String? {
        val regex = """^\S+\s+\S+\s+(.*)""".toRegex()
        return regex.find(userText)?.groupValues?.get(1)
    }

    private fun extractUserTranslateType(userText: String): String? {
        val regex = """^\S+\s+(\S+)""".toRegex()
        return regex.find(userText)?.groupValues?.get(1)
    }

    private fun parseApiResponse(jsonString: String): ApiResponse {
        val jsonElement = Json.parseToJsonElement(jsonString)
        val jsonObject = jsonElement.jsonObject

        return if ("error" in jsonObject) {
            Json.decodeFromJsonElement(ErrorResponse.serializer(), jsonElement)
        } else {
            Json.decodeFromJsonElement(SuccessResponse.serializer(), jsonElement)
        }
    }

    override fun provideCommand(): String = "translate"

    override fun help(): String =
        buildMessage {
            title("TranslateModule Help")
            text("Wanna talk like a pirate? Try using:\n?translate pirate how is your day going?")
            text("Wanna talk like a yoda? Try using:\n?translate yoda how is your day going?")
        }
}
