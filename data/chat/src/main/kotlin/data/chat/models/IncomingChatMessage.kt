package data.chat.models

data class IncomingChatMessage(
    val channeId: String,
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    var userText: String = "",
)

data class OutgoingChatMessage(
    val channelId: String,
    val text: String,
)

data class ChatUser(val userId: String)
