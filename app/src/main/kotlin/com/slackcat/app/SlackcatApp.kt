package com.slackcat.app

import com.slackcat.app.modules.date.DateModule
import com.slackcat.app.modules.kudos.KudosModule
import com.slackcat.app.modules.ping.PingModule
import com.slackcat.app.modules.status.StatusModule
import com.features.slackcat.SlackcatBot
import com.features.slackcat.models.SlackcatModule
import kotlin.reflect.KClass


class SlackcatApp {
    val modules: Array<KClass<out SlackcatModule>> = arrayOf(
        DateModule::class,
        KudosModule::class,
        PingModule::class,
        StatusModule::class
    )


    fun onCreate(args: String?) {
        val slackcatBot = SlackcatBot(
            modules = modules,
            coroutineScope = SlackcatAppGraph.globalScope
        )
        slackcatBot.start(args)
    }
}

