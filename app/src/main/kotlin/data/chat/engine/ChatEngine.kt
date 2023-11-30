package data.chat.engine

import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import kotlinx.coroutines.flow.SharedFlow

interface ChatEngine {
    suspend fun connect()
    suspend fun sendMessage(message: OutgoingChatMessage)
    suspend fun disconnect()
    suspend fun eventFlow(): SharedFlow<IncomingChatMessage>
    fun provideEngineName(): String
}