package features.common


import data.chat.models.IncomingChatMessage
import org.jetbrains.exposed.sql.Table


abstract class FeatureModule {
    abstract fun onInvoke(incomingChatMessage: IncomingChatMessage)
    abstract fun provideCommand(): String
}


interface StorageClient {
    fun provideTable(): Table
}