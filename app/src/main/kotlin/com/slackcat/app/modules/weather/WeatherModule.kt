package com.slackcat.app.modules.weather

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text

class WeatherModule : SlackcatModule() {
    private val weatherClient = WeatherClient()
    private val weatherMessageFactory = WeatherMessageFactory()
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        when (val result = weatherClient.getForecast(incomingChatMessage.userText)) {
            null -> sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    message = text("Could not find location ${incomingChatMessage.userText}.\nVerify it <https://geocoding-api.open-meteo.com/v1/search?name=04011&country=US|here>.\nYou may need to use a bigger city.")
                )
            )

            else -> {
                val richMessage = weatherMessageFactory.makeMessage(result)
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        message = text(richMessage)
                    )
                )
            }
        }
    }

    override fun provideCommand(): String = "weather"

    override fun help(): String = buildMessage {
        title("WeatherModule Help")
        text("This module is will give you weather data from NWS")
        text("Usage: ?weather <zipcode>")
    }
}