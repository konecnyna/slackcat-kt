package app

import app.AppGraph.globalScope
import app.common.Router
import chat.ChatGraph
import chat.models.ChatClient
import chat.engine.cli.CliChatEngine
import chat.engine.slack.SlackChatEngine
import chat.models.OutgoingChatMessage
import database.DatabaseGraph.connectDatabase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class App {
    private val router = Router()

    fun onCreate(args: String?) {
        ChatGraph.chatEngine = if (!args.isNullOrEmpty()) {
            CliChatEngine(args)
        } else {
            SlackChatEngine()
        }

        ChatGraph.chatClient = object : ChatClient {
            override fun sendMessage(message: OutgoingChatMessage) {
                globalScope.launch { ChatGraph.chatEngine.sendMessage(message) }
            }
        }

        connectDatabase()

        runBlocking {
            println("Starting slackcat using ${ChatGraph.chatEngine.provideEngineName()} engine")
            ChatGraph.chatEngine.connect()
            ChatGraph.chatEngine.eventFlow().collect {
                val handled = router.onMessage(it)
                if (!handled && ChatGraph.chatEngine is CliChatEngine) {
                    throw Error(CliChatEngine.commandNotHandledErrorMessage)
                }
            }
        }
    }
}