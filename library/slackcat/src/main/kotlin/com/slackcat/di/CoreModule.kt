package com.slackcat.di

import com.slackcat.DefaultDatasourceFactory
import com.slackcat.Engine
import com.slackcat.common.SlackcatConfig
import com.slackcat.common.SlackcatEvent
import com.slackcat.network.NetworkClient
import com.slackcat.network.NetworkGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.qualifier.named
import org.koin.dsl.module
import javax.sql.DataSource

val coreModule =
    module {
        // CoroutineScope
        single<CoroutineScope> { CoroutineScope(Dispatchers.IO) }

        // Event flow for module communication
        single<MutableSharedFlow<SlackcatEvent>> { MutableSharedFlow() }
        single<SharedFlow<SlackcatEvent>>(
            named("eventsFlow"),
        ) { get<MutableSharedFlow<SlackcatEvent>>().asSharedFlow() }

        // Engine configuration - default from ENV or can be overridden
        single<Engine> {
            when (System.getenv("ENV")) {
                "PRODUCTION" -> Engine.ChatClient.Slack
                else -> Engine.CLI
            }
        }

        // NetworkClient - can be overridden
        single<NetworkClient> { NetworkGraph.networkClient }

        // DataSource factory - can be overridden
        single { DefaultDatasourceFactory() }

        // DataSource - can be overridden by providing custom DataSource
        // Defaults to SQLite for CLI, PostgreSQL for Slack
        single<DataSource> {
            get<DefaultDatasourceFactory>().makeDatabaseSource(get(), get<SlackcatConfig>().databaseConfig)
        }
    }
