package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.text
import java.text.SimpleDateFormat
import java.util.Date

class DateModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val dateFormat = SimpleDateFormat("MMMM dd, hh:mm a")
        val currentDate = Date()
        val dateString = dateFormat.format(currentDate)

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text("Currently, it's $dateString where I am"),
            ),
        )
    }

    override fun provideCommand(): String = "date"

    override fun help(): BotMessage =
        buildMessage {
            heading("DateModule Help")
            text("This module returns the local date where the server is which is useful for debugging")
        }
}
