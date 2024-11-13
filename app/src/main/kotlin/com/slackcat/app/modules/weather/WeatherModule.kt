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
        weatherClient.getForecast("10280")?.let {
            val richMessage = weatherMessageFactory.makeMessage(it)
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    text = richMessage
                )
            )
        }
    }

    override fun provideCommand(): String = "weather"

    override fun help(): String = buildMessage {
        title("WeatherModule Help")
        text("This module is will give you weather data from NWS")
        text("Usage: ?weather <zipcode>")
    }
}