package data.chat.models

interface ChatClient {
    fun sendMessage(message: OutgoingChatMessage)
}
