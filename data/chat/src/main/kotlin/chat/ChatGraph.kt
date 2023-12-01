package chat

import chat.engine.ChatEngine
import chat.models.ChatClient

object ChatGraph {
    lateinit var chatEngine: ChatEngine
    lateinit var chatClient: ChatClient
}