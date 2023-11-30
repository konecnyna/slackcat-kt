package app.models

data class Message(
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    val userText: String,
)

data class ChatUser(val userId: String)