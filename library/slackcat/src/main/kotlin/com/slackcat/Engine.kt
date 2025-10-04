package com.slackcat

sealed interface Engine {
    data object Slack : Engine

    data object Cli : Engine
}
