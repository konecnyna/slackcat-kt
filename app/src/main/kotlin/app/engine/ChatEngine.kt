package app.engine

import kotlinx.coroutines.flow.SharedFlow

interface ChatEngine {
    suspend fun connect()
    suspend fun sendMessage(message: String)
    suspend fun disconnect()
    suspend fun eventFlow(): SharedFlow<String>
    fun provideEngineName(): String
}