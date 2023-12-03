package app

import app.common.Router
import app.common.SlackcatBot
import data.chat.ChatGraph
import data.chat.engine.cli.CliChatEngine


class App {
    private val router = Router()
    private val slackcatBot = SlackcatBot()

    fun onCreate(args: String?) {
        slackcatBot.start(args) {
            val handled = router.onMessage(it)
            if (!handled && ChatGraph.chatEngine is CliChatEngine) {
                throw Error(CliChatEngine.commandNotHandledErrorMessage)
            }
        }
    }
}

