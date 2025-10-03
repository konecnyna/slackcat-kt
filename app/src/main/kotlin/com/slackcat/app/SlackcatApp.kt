package com.slackcat.app

import io.github.cdimascio.dotenv.dotenv
import com.slackcat.SlackcatBot
import com.slackcat.app.di.appModule
import com.slackcat.di.coreModule
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext
import javax.sql.DataSource
import kotlin.reflect.KClass

class SlackcatApp {
    fun onCreate(args: String?) {
        // Load environment variables from .env file
        dotenv {
            directory = "../"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        // Initialize Koin
        startKoin {
            modules(coreModule, appModule)
        }

        // Get dependencies from Koin
        val koin = GlobalContext.get()
        val coroutineScope = koin.get<CoroutineScope>()
        val dataSource = koin.get<DataSource>()
        val networkClient = koin.get<NetworkClient>()
        val moduleClasses = koin.get<List<KClass<out SlackcatModule>>>()

        // Create SlackcatBot with Koin dependencies
        val slackcatBot = SlackcatBot(
            modulesClasses = moduleClasses.toTypedArray(),
            coroutineScope = coroutineScope,
            databaseConfig = dataSource,
            networkClient = networkClient
        )
        slackcatBot.start(args)
    }
}
