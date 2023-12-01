package chat.models

interface ChatClient {
    fun sendMessage(message: OutgoingChatMessage)
}
