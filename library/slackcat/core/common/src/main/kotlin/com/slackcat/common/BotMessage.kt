package com.slackcat.common

/**
 * Platform-agnostic message structure that can be sent by the bot.
 * Supports multiple platforms (Slack, CLI, Discord, Teams, etc.) through platform-specific converters.
 */
data class BotMessage(
    val elements: List<MessageElement>,
    val style: MessageStyle? = null,
)
