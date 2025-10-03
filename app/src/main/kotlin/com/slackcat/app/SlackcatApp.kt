package com.slackcat.app

import io.github.cdimascio.dotenv.dotenv
import com.slackcat.SlackcatBot
import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.deploybot.DeployBotModule
import com.slackcat.app.modules.emojitext.EmojiTextModule
import com.slackcat.models.SlackcatModule
import com.slackcat.modules.SlackcatModules
import kotlin.reflect.KClass

class SlackcatApp {
    val dataSourceFactory = DatasourceFactory()

    // Mix official modules from SlackcatModules with app-specific custom modules
    val modules: List<KClass<out SlackcatModule>> = SlackcatModules.all + listOf(
        BigHipsModule::class,
        DeployBotModule::class,
        EmojiTextModule::class
    )

    fun onCreate(args: String?) {
        // Load environment variables from .env file
        dotenv {
            directory = "../"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        val slackcatBot = SlackcatBot(
            modulesClasses = modules,
            coroutineScope = SlackcatAppGraph.globalScope,
            databaseConfig = dataSourceFactory.makeDatabaseSource(SlackcatAppGraph.ENV),
            networkClient = SlackcatAppGraph.networkClient
        )
        slackcatBot.start(args)
    }
}
