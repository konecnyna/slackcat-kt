package com.slackcat.app

import com.slackcat.SlackcatBot
import com.slackcat.app.di.appModule
import com.slackcat.chat.models.BotIcon
import com.slackcat.common.SlackcatAppDefaults
import com.slackcat.common.SlackcatConfig
import com.slackcat.di.coreModule
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient
import io.github.cdimascio.dotenv.dotenv
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.time.LocalDate
import java.time.Month
import javax.sql.DataSource
import kotlin.reflect.KClass

class SlackcatApp {
    fun onCreate(args: String?) {
        dotenv {
            directory = "../"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        startKoin {
            allowOverride(true)
            modules(coreModule, appModule)
        }

        val koin = GlobalContext.get()
        val dataSource = koin.get<DataSource>()
        val networkClient = koin.get<NetworkClient>()
        val moduleClasses = koin.get<List<KClass<out SlackcatModule>>>()

        val slackcatBot = SlackcatBot(
            modulesClasses = moduleClasses.toTypedArray(),
            databaseConfig = dataSource,
            networkClient = networkClient,
        )
        slackcatBot.start(args)
    }
}
