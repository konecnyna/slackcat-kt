package app

import app.AppGraph.globalScope
import app.common.ChatClient
import app.common.Router
import app.engine.ChatEngine
import app.engine.CliChatEngine
import app.engine.SlackChatEngine
import data.database.DatabaseGraph
import data.database.DatabaseGraph.connectDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class App {
    private val router = Router()

    fun onCreate(args: String?) {
        AppGraph.chatEngine = if (!args.isNullOrEmpty()) {
            CliChatEngine(args)
        } else {
            SlackChatEngine()
        }

        AppGraph.chatClient = object : ChatClient {
            override fun sendMessage(message: String) {
                globalScope.launch { AppGraph.chatEngine.sendMessage(message) }
            }
        }
        
        connectDatabase()

        runBlocking {
            println("Starting slackcat using ${AppGraph.chatEngine.provideEngineName()} engine")
            AppGraph.chatEngine.connect()
            AppGraph.chatEngine.eventFlow().collect {
                val handled = router.onMessage(it)
                if (!handled && AppGraph.chatEngine is CliChatEngine) {
                    throw Error(CliChatEngine.commandNotHandledErrorMessage)
                }
            }
        }
    }
}