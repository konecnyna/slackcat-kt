package com.slackcat

sealed interface Engine {
    data object CLI : Engine

    sealed interface ChatClient : Engine {
        data object Slack : ChatClient
    }
}
