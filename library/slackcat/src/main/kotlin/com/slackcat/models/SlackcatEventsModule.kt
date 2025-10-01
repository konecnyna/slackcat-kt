package com.slackcat.models

import com.slackcat.common.SlackcatEvent
import org.jetbrains.exposed.sql.Table

interface SlackcatEventsModule {
    suspend fun onEvent(event: SlackcatEvent)
}
