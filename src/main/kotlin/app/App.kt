package app

import app.engine.ChatEngine
import app.engine.CliChatEngine
import app.engine.SlackChatEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class App {
    companion object {
        val globalScope = CoroutineScope(Dispatchers.IO)
    }

    fun onCreate(args: String?) {
        val chatEngine: ChatEngine = if (!args.isNullOrEmpty()) {
            CliChatEngine(args)
        } else {
            SlackChatEngine()
        }

        val router = Router(object : ChatClient {
            override fun sendMessage(message: String) {
                globalScope.launch { chatEngine.sendMessage(message) }
            }
        })

        runBlocking {
            println("Starting slackcat using ${chatEngine.provideEngineName()} engine")
            chatEngine.connect()
            chatEngine.eventFlow().collect {
                val handled = router.onMessage(it)
                if (!handled && chatEngine is CliChatEngine) {
                    throw Error(CliChatEngine.commandNotHandledErrorMessage)
                }
            }
        }
    }
}