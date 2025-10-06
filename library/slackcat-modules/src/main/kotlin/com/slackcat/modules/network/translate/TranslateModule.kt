package com.slackcat.modules.network.translate

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.modules.ErrorResponseFunTranslation
import com.slackcat.modules.FunTranslationApiResponse
import com.slackcat.modules.SuccessResponseFunTranslation
import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class TranslateModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        coroutineScope.launch {
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
                    is ErrorResponseFunTranslation -> response.error.message
                    is SuccessResponseFunTranslation -> response.contents.translated
                    null -> "Translate failed: sorry bub"
                }

            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    message = text(outgoingText),
                ),
            )
        }
    }

    private suspend fun post(
        text: String,
        translationType: String,
    ): FunTranslationApiResponse? {
        return runCatching {
            val response =
                networkClient.post(
                    "https://api.funtranslations.com/translate/$translationType",
                    """{"text":"$text"}""",
                    emptyMap(),
                )
            deserialize(response)
        }.getOrNull()
    }

    private fun deserialize(response: String): FunTranslationApiResponse {
        return when (parseApiResponse(response)) {
            is SuccessResponseFunTranslation ->
                json.decodeFromString(
                    SuccessResponseFunTranslation.serializer(),
                    response,
                )

            is ErrorResponseFunTranslation -> json.decodeFromString(ErrorResponseFunTranslation.serializer(), response)
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

    private fun parseApiResponse(jsonString: String): FunTranslationApiResponse {
        val jsonElement = Json.parseToJsonElement(jsonString)
        val jsonObject = jsonElement.jsonObject

        return if ("error" in jsonObject) {
            Json.decodeFromJsonElement(ErrorResponseFunTranslation.serializer(), jsonElement)
        } else {
            Json.decodeFromJsonElement(SuccessResponseFunTranslation.serializer(), jsonElement)
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
