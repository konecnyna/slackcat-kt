package features.common

import app.ChatClient
import app.Router

abstract class ChatModule {
    abstract fun onInvoke(message: Router.Message)
    abstract fun provideCommand(): String
    lateinit var chatClient: ChatClient
}