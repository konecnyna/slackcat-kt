package data.chat.models

data class IncomingChatMessage(
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    var userText: String = "",
)

data class OutgoingChatMessage(
    val text: String,
)

data class ChatUser(val userId: String)