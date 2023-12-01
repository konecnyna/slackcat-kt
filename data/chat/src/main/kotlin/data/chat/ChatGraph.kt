package data.chat

import data.chat.engine.ChatEngine
import data.chat.models.ChatClient

object ChatGraph {
    lateinit var chatEngine: ChatEngine
    lateinit var chatClient: ChatClient
}
