package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.SlackcatModule

class PingModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val text =
            when (incomingChatMessage.command) {
                "bing" -> "bong"
                "ding" -> "dong"
                "ring" -> "wrong"
                else -> "pong"
            }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage(text),
            ),
        )
    }

    override fun provideCommand(): String = "ping"

    override fun aliases(): List<String> =
        listOf(
            "bing",
            "ding",
            "ring",
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("Ping Help")
            text("This module is for debugging. If slackcat is running ?ping will return ?pong")
        }
}
