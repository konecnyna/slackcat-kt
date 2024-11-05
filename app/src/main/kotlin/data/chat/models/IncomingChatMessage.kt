package data.chat.models

data class IncomingChatMessage(
    val channelId: String,
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    var userText: String = "",
    var threadId: String? = null,
)

data class OutgoingChatMessage(
    val channel: String,
    val text: String,
)

data class ChatUser(val userId: String)