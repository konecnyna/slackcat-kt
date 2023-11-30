package app

import app.AppGraph.globalScope
import data.chat.models.ChatClient
import app.common.Router
import data.chat.engine.cli.CliChatEngine
import data.chat.engine.slack.SlackChatEngine
import data.chat.models.OutgoingChatMessage
import data.database.DatabaseGraph.connectDatabase
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
            override fun sendMessage(message: OutgoingChatMessage) {
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