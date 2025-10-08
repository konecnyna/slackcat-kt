package com.slackcat.modules.network.weather

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient

class WeatherModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val weatherClient by lazy { WeatherClient(networkClient) }
    private val weatherMessageFactory = WeatherMessageFactory()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        when (val result = weatherClient.getForecast(incomingChatMessage.userText)) {
            null ->
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        content =
                            textMessage(
                                "Could not find location ${incomingChatMessage.userText}.\n" +
                                    "Verify it " +
                                    "<https://geocoding-api.open-meteo.com/v1/search?name=04011&country=US|here>.\n" +
                                    "You may need to use a bigger city.",
                            ),
                    ),
                )

            else -> {
                val richMessage = weatherMessageFactory.makeMessage(result)
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        content = textMessage(richMessage),
                    ),
                )
            }
        }
    }

    override fun commandInfo() = CommandInfo(command = "weather")

    override fun help(): BotMessage =
        buildMessage {
            heading("WeatherModule Help")
            text("This module is will give you weather data from NWS")
            text("Usage: ?weather <zipcode>")
        }
}
