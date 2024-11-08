package com.slackcat.app

import com.slackcat.SlackcatBot
import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.date.DateModule
import com.slackcat.app.modules.kudos.KudosModule
import com.slackcat.app.modules.ping.PingModule
import com.slackcat.app.modules.status.StatusModule
import com.slackcat.app.modules.translate.TranslateModule
import com.slackcat.models.SlackcatModule
import kotlin.reflect.KClass

class SlackcatApp {
    val modules: Array<KClass<out SlackcatModule>> =
        arrayOf(
            DateModule::class,
            KudosModule::class,
            PingModule::class,
            StatusModule::class,
            BigHipsModule::class,
            TranslateModule::class,
        )

    fun onCreate(args: String?) {
        val slackcatBot =
            SlackcatBot(
                modules = modules,
                coroutineScope = SlackcatAppGraph.globalScope,
            )
        slackcatBot.start(args)
    }
}
