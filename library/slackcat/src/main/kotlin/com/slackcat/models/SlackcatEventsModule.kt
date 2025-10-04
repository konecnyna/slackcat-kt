package com.slackcat.models

import com.slackcat.common.SlackcatEvent

interface SlackcatEventsModule {
    suspend fun onEvent(event: SlackcatEvent)
}
