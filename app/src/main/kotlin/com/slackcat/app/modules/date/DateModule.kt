package com.slackcat.app.modules.date

import com.features.slackcat.models.SlackcatModule
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import java.text.SimpleDateFormat
import java.util.Date

class DateModule : SlackcatModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val dateFormat = SimpleDateFormat("MMMM dd, hh:mm a")
        val currentDate = Date()
        val dateString = dateFormat.format(currentDate)

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channeId,
                text = "Currently, it's $dateString where I am",
            ),
        )
    }

    override fun provideCommand(): String = "date"
}
