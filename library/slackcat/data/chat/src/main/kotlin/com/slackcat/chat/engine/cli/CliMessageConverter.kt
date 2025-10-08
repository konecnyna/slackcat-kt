package com.slackcat.chat.engine.cli

import com.slackcat.common.BotMessage
import com.slackcat.common.MessageElement
import com.slackcat.common.MessageStyle
import com.slackcat.common.TextStyle

/**
 * Converts platform-agnostic BotMessage to plain text for CLI output.
 */
class CliMessageConverter {
    fun toPlainText(message: BotMessage): String {
        val builder = StringBuilder()

        // Add style prefix if present
        message.style?.let { style ->
            builder.appendLine(getStylePrefix(style))
        }

        // Convert each element
        message.elements.forEach { element ->
            when (element) {
                is MessageElement.Text -> {
                    val formattedText = formatText(element)
                    builder.appendLine(formattedText)
                }
                is MessageElement.Heading -> {
                    builder.appendLine("=".repeat(60))
                    builder.appendLine(element.content.uppercase())
                    builder.appendLine("=".repeat(60))
                }
                is MessageElement.Image -> {
                    builder.appendLine("[Image: ${element.altText}]")
                    builder.appendLine("  URL: ${element.url}")
                }
                is MessageElement.Divider -> {
                    builder.appendLine("-".repeat(60))
                }
                is MessageElement.KeyValueList -> {
                    element.items.forEach { item ->
                        builder.appendLine("  ${item.key}: ${item.value}")
                    }
                }
                is MessageElement.Context -> {
                    builder.appendLine("  ℹ️  ${element.content}")
                }
            }
        }

        return builder.toString().trimEnd()
    }

    private fun formatText(text: MessageElement.Text): String {
        return when (text.style) {
            TextStyle.BOLD -> "** ${text.content} **"
            TextStyle.CODE -> "`${text.content}`"
            TextStyle.QUOTE -> "  > ${text.content}"
            TextStyle.NORMAL -> text.content
        }
    }

    private fun getStylePrefix(style: MessageStyle): String {
        return when (style) {
            MessageStyle.SUCCESS -> "✅ SUCCESS"
            MessageStyle.WARNING -> "⚠️  WARNING"
            MessageStyle.ERROR -> "❌ ERROR"
            MessageStyle.INFO -> "ℹ️  INFO"
            MessageStyle.NEUTRAL -> ""
        }
    }
}
