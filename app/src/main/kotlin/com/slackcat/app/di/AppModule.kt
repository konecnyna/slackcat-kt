package com.slackcat.app.di

import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.deploybot.DeployBotModule
import com.slackcat.app.modules.jeopardy.JeopardyModule
import com.slackcat.chat.models.BotIcon
import com.slackcat.common.DatabaseConfig
import com.slackcat.common.SlackcatAppDefaults
import com.slackcat.common.SlackcatConfig
import com.slackcat.models.SlackcatModule
import com.slackcat.modules.SlackcatModules
import com.slackcat.modules.simple.emojitext.EmojiTextModule
import org.koin.dsl.module
import java.time.LocalDate
import java.time.Month
import kotlin.reflect.KClass

val appModule =
    module {
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

        // SlackcatConfig with custom date-based bot names/icons and database config
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
                },
                databaseConfig =
                    DatabaseConfig(
                        url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432",
                        name = System.getenv("DATABASE_NAME") ?: "slackcat",
                        username = System.getenv("DATABASE_USER") ?: "",
                        password = System.getenv("DATABASE_PASSWORD") ?: "",
                    ),
            )
        }
    }
