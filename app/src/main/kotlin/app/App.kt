package app

import app.modules.date.DateModule
import app.modules.kudos.KudosModule
import app.modules.ping.PingModule
import app.modules.status.StatusModule
import features.slackcat.SlackcatBot
import features.slackcat.models.SlackcatModule
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


class App {
    val modules: Array<KClass<out SlackcatModule>> = arrayOf(
        DateModule::class,
        KudosModule::class,
        PingModule::class,
        StatusModule::class
    )


    fun onCreate(args: String?) {
        val slackcatBot = SlackcatBot(
            modules = modules,
            coroutineScope = AppGraph.globalScope
        )
        slackcatBot.start(args)
    }
}

