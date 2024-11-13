package com.slackcat.app.modules.weather

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.chat.models.outgoingMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage

class WeatherModule : SlackcatModule() {
    private val weatherClient = WeatherClient()
    private val weatherMessageFactory = WeatherMessageFactory()
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        when (val result = weatherClient.getForecast(incomingChatMessage.userText)) {
            null -> postHelpMessage(channelId = incomingChatMessage.channelId)
            else -> {
                val richMessage = weatherMessageFactory.makeMessage(result)
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        text = richMessage
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