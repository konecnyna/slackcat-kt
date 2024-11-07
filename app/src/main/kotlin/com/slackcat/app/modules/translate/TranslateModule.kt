package com.slackcat.app.modules.translate

import com.features.slackcat.models.SlackcatModule
import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class TranslateModule : SlackcatModule() {
    @Serializable
    sealed class ApiResponse

    @Serializable
    data class SuccessResponse(
        val success: Success,
        val contents: Contents,
    ) : ApiResponse()

    @Serializable
    data class ErrorResponse(
        val error: Error,
    ) : ApiResponse()

    @Serializable
    data class Success(val total: Int)

    @Serializable
    data class Contents(val translated: String, val text: String, val translation: String)

    @Serializable
    data class Error(val code: Int, val message: String)

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
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
                buildString {
                    if (response is SuccessResponse) {
                        append(response.contents.translated)
                    } else if (response is ErrorResponse) {
                        append(response.error.message)
                    } else {
                        append("Translate failed: sorry bub")
                    }
                }

            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channeId,
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
            is SuccessResponse -> Json { ignoreUnknownKeys = true }.decodeFromString(SuccessResponse.serializer(), response)
            is ErrorResponse -> Json { ignoreUnknownKeys = true }.decodeFromString(ErrorResponse.serializer(), response)
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
}
