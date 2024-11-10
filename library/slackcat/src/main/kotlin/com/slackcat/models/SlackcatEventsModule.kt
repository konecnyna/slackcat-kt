package com.slackcat.models

import com.slackcat.common.SlackcatEvent
import org.jetbrains.exposed.sql.Table

interface SlackcatEventsModule {
    fun onEvent(event: SlackcatEvent)
}
