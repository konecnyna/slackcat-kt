package features.common

import data.chat.models.IncomingChatMessage

abstract class FeatureModule {
    abstract fun onInvoke(incomingChatMessage: IncomingChatMessage)

    abstract fun provideCommand(): String
}
