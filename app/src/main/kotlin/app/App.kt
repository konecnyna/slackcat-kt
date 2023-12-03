package app

import features.bot.SlackcatBot


class App {
    private val slackcatBot = SlackcatBot()

    fun onCreate(args: String?) {
        slackcatBot.start(args)
    }
}

