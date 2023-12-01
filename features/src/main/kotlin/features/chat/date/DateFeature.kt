package features.chat.date

import chat.ChatGraph.chatClient
import chat.models.IncomingChatMessage
import chat.models.OutgoingChatMessage
import features.common.FeatureModule
import java.text.SimpleDateFormat
import java.util.*

class DateFeature: FeatureModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val dateFormat = SimpleDateFormat("MMMM dd, hh:mm a")
        val currentDate = Date()
        val dateString = dateFormat.format(currentDate)

        chatClient.sendMessage(
            OutgoingChatMessage("Currently, it's $dateString where I am")
        )
    }

    override fun provideCommand(): String = "date"
}