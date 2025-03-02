package com.slackcat.app

import com.slackcat.SlackcatBot
import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.cryptoprice.CryptoPriceModule
import com.slackcat.app.modules.date.DateModule
import com.slackcat.app.modules.deploybot.DeployBotModule
import com.slackcat.app.modules.emoji.EmojiModule
import com.slackcat.app.modules.jeopardy.JeopardyModule
import com.slackcat.app.modules.emojisentence.EmojiSentenceModule
import com.slackcat.app.modules.emojitext.EmojiTextModule
import com.slackcat.app.modules.framer.FrameModule
import com.slackcat.app.modules.kudos.KudosModule
import com.slackcat.app.modules.learn.LearnModule
import com.slackcat.app.modules.ping.PingModule
import com.slackcat.app.modules.pokecat.PokeCatModule
import com.slackcat.app.modules.radar.RadarModule
import com.slackcat.app.modules.status.StatusModule
import com.slackcat.app.modules.summon.SummonModule
import com.slackcat.app.modules.translate.TranslateModule
import com.slackcat.app.modules.weather.WeatherModule
import com.slackcat.models.SlackcatModule
import kotlin.reflect.KClass

class SlackcatApp {
    val dataSourceFactory = DatasourceFactory()
    val modules: Array<KClass<out SlackcatModule>> = arrayOf(
        DateModule::class,
        KudosModule::class,
        PingModule::class,
        StatusModule::class,
        BigHipsModule::class,
        TranslateModule::class,
        PokeCatModule::class,
        LearnModule::class,
        SummonModule::class,
        DeployBotModule::class,
        EmojiModule::class,
        WeatherModule::class,
        JeopardyModule::class,
        RadarModule::class,
        EmojiSentenceModule::class,
        EmojiTextModule::class,
        CryptoPriceModule::class,
        FrameModule::class
    )

    fun onCreate(args: String?) {


        val slackcatBot = SlackcatBot(
            modulesClasses = modules,
            coroutineScope = SlackcatAppGraph.globalScope,
            databaseConfig = dataSourceFactory.makeDatabaseSource(SlackcatAppGraph.ENV)
        )
        slackcatBot.start(args)
    }
}
