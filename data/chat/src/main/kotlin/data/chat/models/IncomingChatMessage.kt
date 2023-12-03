package data.chat.models

data class IncomingChatMessage(
    val channeId: String,
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
) {
    val command: String? = """\?\s*(\w+)""".toRegex().find(rawMessage)?.groups?.get(1)?.value
    val userText: String = rawMessage.replace(command ?: "", "")
}

data class OutgoingChatMessage(
    val channelId: String,
    val text: String,
)

data class ChatUser(val userId: String)
