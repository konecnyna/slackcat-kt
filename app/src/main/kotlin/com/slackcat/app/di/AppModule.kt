package com.slackcat.app.di

import com.slackcat.app.DatasourceFactory
import com.slackcat.app.Environment
import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.deploybot.DeployBotModule
import com.slackcat.chat.models.BotIcon
import com.slackcat.common.SlackcatAppDefaults
import com.slackcat.common.SlackcatConfig
import com.slackcat.modules.simple.emojitext.EmojiTextModule
import com.slackcat.app.modules.jeopardy.JeopardyModule
import com.slackcat.models.SlackcatModule
import com.slackcat.modules.SlackcatModules
import com.slackcat.network.NetworkClient
import com.slackcat.network.NetworkGraph
import org.koin.dsl.module
import java.time.LocalDate
import java.time.Month
import javax.sql.DataSource
import kotlin.reflect.KClass

val appModule = module {
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

        // Override SlackcatConfig with custom date-based bot names/icons
        single<SlackcatConfig> {
            SlackcatConfig(
                botNameProvider = {
                    when (LocalDate.now().month) {
                        Month.DECEMBER -> "HolidayCat"
                        Month.OCTOBER -> "SpookyCat"
                        Month.JULY -> "FreedomCat"
                        else -> SlackcatAppDefaults.DEFAULT_BOT_NAME
                    }
                },
                botIconProvider = {
                    when (LocalDate.now().month) {
                        Month.DECEMBER -> BotIcon.BotEmojiIcon(":santa:")
                        Month.OCTOBER -> BotIcon.BotImageIcon("https://i.imgur.com/8cxx5in.png")
                        Month.JULY -> BotIcon.BotEmojiIcon(":flag-us:")
                        else -> BotIcon.BotImageIcon(SlackcatAppDefaults.DEFAULT_BOT_IMAGE_ICON)
                    }
                }
            )
        }
    }
