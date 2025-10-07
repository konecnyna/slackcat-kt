package com.slackcat.common

/**
 * Platform-agnostic message structure that can be sent by the bot.
 * This replaces the Slack-specific RichTextMessage with a more universal model.
 */
data class BotMessage(
    val elements: List<MessageElement>,
    val style: MessageStyle? = null,
)
