package com.slackcat.di

import com.slackcat.common.SlackcatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {
    // CoroutineScope
    single<CoroutineScope> { CoroutineScope(Dispatchers.IO) }

    // Event flow for module communication
    single<MutableSharedFlow<SlackcatEvent>> { MutableSharedFlow() }
    single<SharedFlow<SlackcatEvent>>(named("eventsFlow")) { get<MutableSharedFlow<SlackcatEvent>>().asSharedFlow() }
}
