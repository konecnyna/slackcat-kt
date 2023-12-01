package features.common

import chat.models.IncomingChatMessage
import org.jetbrains.exposed.sql.Table


abstract class FeatureModule {
    abstract fun onInvoke(incomingChatMessage: IncomingChatMessage)
    abstract fun provideCommand(): String
}