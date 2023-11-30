package features.chat.date

import app.Router
import features.common.ChatModule
import java.text.SimpleDateFormat
import java.util.*

class DateFeature: ChatModule() {
    override fun onInvoke(message: Router.Message) {
        val dateFormat = SimpleDateFormat("MMMM dd, hh:mm a")
        val currentDate = Date()
        val dateString = dateFormat.format(currentDate)

        chatClient.sendMessage("Currently, it's $dateString where I am")
    }

    override fun provideCommand(): String = "date"
}