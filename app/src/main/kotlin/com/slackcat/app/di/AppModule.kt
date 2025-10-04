package com.slackcat.app.di

import com.slackcat.app.DatasourceFactory
import com.slackcat.app.Environment
import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.deploybot.DeployBotModule
import com.slackcat.modules.simple.emojitext.EmojiTextModule
import com.slackcat.app.modules.jeopardy.JeopardyModule
import com.slackcat.models.SlackcatModule
import com.slackcat.modules.SlackcatModules
import com.slackcat.network.NetworkClient
import com.slackcat.network.NetworkGraph
import org.koin.dsl.module
import javax.sql.DataSource
import kotlin.reflect.KClass

val appModule =
    module {
        // Environment configuration
        single<Environment> {
            when (System.getenv("ENV")) {
                "PRODUCTION" -> Environment.Production
                else -> Environment.Development
            }
        }

        // DataSource factory
        single { DatasourceFactory() }

        // DataSource - environment-specific
        single<DataSource> {
            get<DatasourceFactory>().makeDatabaseSource(get())
        }

        // NetworkClient
        single<NetworkClient> { NetworkGraph.networkClient }

        // Module classes - combining library modules with app-specific modules
        single<List<KClass<out SlackcatModule>>> {
            SlackcatModules.all +
                listOf(
                    BigHipsModule::class,
                    DeployBotModule::class,
                    EmojiTextModule::class,
                    JeopardyModule::class,
                )
        }
    }
