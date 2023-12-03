package app.modules.date

import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.slackcat.models.SlackcatModule
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
                text= "Currently, it's $dateString where I am"
            ),
        )
    }

    override fun provideCommand(): String = "date"
}
