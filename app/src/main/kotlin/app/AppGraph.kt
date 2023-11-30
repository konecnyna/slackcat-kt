package app

import app.common.ChatClient
import app.engine.ChatEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * A very simple global singleton dependency graph.
 *
 * For a real app, you would use something like Hilt/Dagger instead.
 */
object AppGraph {
    lateinit var chatEngine: ChatEngine
    lateinit var chatClient: ChatClient
    val globalScope = CoroutineScope(Dispatchers.IO)
}