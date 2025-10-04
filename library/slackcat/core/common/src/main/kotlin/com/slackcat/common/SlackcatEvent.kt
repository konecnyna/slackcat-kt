package com.slackcat.common

sealed interface SlackcatEvent {
    data object STARTED : SlackcatEvent
}
