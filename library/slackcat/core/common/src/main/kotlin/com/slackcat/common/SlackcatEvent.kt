package com.slackcat.common

sealed interface SlackcatEvent {
    data object STARTED : SlackcatEvent

    data class MessageReceived(
        val userId: String,
        val channelId: String,
        val text: String,
        val timestamp: String,
        val threadTimestamp: String?,
    ) : SlackcatEvent

    data class ReactionAdded(
        val userId: String,
        val reaction: String,
        val channelId: String,
        val messageTimestamp: String,
        val itemUserId: String?,
        val eventTimestamp: String,
    ) : SlackcatEvent

    data class ReactionRemoved(
        val userId: String,
        val reaction: String,
        val channelId: String,
        val messageTimestamp: String,
        val itemUserId: String?,
        val eventTimestamp: String,
    ) : SlackcatEvent
}
