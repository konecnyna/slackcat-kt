package com.slackcat.app

import com.slackcat.SlackcatBot
import com.slackcat.app.modules.bighips.BigHipsModule
import com.slackcat.app.modules.date.DateModule
import com.slackcat.app.modules.kudos.KudosModule
import com.slackcat.app.modules.learn.LearnModule
import com.slackcat.app.modules.ping.PingModule
import com.slackcat.app.modules.pokecat.PokeCatModule
import com.slackcat.app.modules.status.StatusModule
import com.slackcat.app.modules.translate.TranslateModule
import com.slackcat.models.SlackcatModule
import org.apache.commons.dbcp2.BasicDataSource
import javax.sql.DataSource
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
            PokeCatModule::class,
            LearnModule::class,
        )

    private fun createDataSource(): DataSource {
        val databaseUrl = "${System.getenv("DATABASE_URL")?: "jdbc:sqlite:"}/${System.getenv("DATABASE_NAME")}"
        val driverName = when {
            databaseUrl.startsWith("jdbc:sqlite") -> "org.sqlite.JDBC"
            databaseUrl.startsWith("jdbc:postgresql") -> "org.postgresql.Driver"
            databaseUrl.startsWith("jdbc:mysql") -> "com.mysql.cj.jdbc.Driver"
            else -> { throw IllegalArgumentException("Unsupported database URL: $databaseUrl") }
        }
        println("Database URL: $databaseUrl")
        println("Driver: $driverName")
        val dataSource = BasicDataSource().apply {
            url = databaseUrl
            username = System.getenv("DATABASE_USER") ?: ""
            password = System.getenv("DATABASE_PASSWORD") ?: ""
            driverClassName = driverName
            maxTotal = 10  // Maximum number of connections in the pool
            maxIdle = 5    // Maximum number of idle connections in the pool
            minIdle = 2    // Minimum number of idle connections in the pool
        }
        println("Datasource: $dataSource")
        return dataSource
    }

    fun onCreate(args: String?) {
        val slackcatBot =
            SlackcatBot(
                modulesClasses = modules,
                coroutineScope = SlackcatAppGraph.globalScope,
                databaseConfig = createDataSource()
            )
        slackcatBot.start(args)
    }
}
