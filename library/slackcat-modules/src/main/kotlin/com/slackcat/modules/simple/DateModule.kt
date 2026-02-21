package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
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
                content = textMessage("Currently, it's $dateString where I am"),
            ),
        )
    }

    override fun commandInfo() = CommandInfo(command = "date")

    override fun help(): BotMessage =
        buildMessage {
            heading("DateModule Help")
            text("This module returns the local date where the server is which is useful for debugging")
        }
}
