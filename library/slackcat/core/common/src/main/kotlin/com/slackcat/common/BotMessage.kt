package com.slackcat.common

/**
 * Platform-agnostic message structure that can be sent by the bot.
 * Supports multiple platforms (Slack, CLI, Discord, Teams, etc.) through platform-specific converters.
 */
data class BotMessage(
    val elements: List<MessageElement>,
    val style: MessageStyle? = null,
) {
    fun toPlainText(): String {
        return elements.joinToString("\n") { element ->
            when (element) {
                is MessageElement.Text -> element.content
                is MessageElement.Heading -> "*${element.content}*"
                is MessageElement.Image -> "[Image: ${element.altText}]"
                is MessageElement.Divider -> "---"
                is MessageElement.KeyValueList ->
                    element.items.joinToString("\n") { "${it.key}: ${it.value}" }
                is MessageElement.Context -> element.content
            }
        }
    }
}
