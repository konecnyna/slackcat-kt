package features.chat.date

import app.AppGraph.chatClient
import app.models.Message
import features.common.FeatureModule
import java.text.SimpleDateFormat
import java.util.*

class DateFeature: FeatureModule() {
    override fun onInvoke(message: Message) {
        val dateFormat = SimpleDateFormat("MMMM dd, hh:mm a")
        val currentDate = Date()
        val dateString = dateFormat.format(currentDate)

        chatClient.sendMessage("Currently, it's $dateString where I am")
    }

    override fun provideCommand(): String = "date"
}