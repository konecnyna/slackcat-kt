package com.slackcat.common

/**
 * Platform-agnostic message builder for creating BotMessages.
 * Use this builder to construct messages that work across all platforms.
 */
class MessageBuilder {
    private val elements = mutableListOf<MessageElement>()

    /**
     * Add plain text to the message.
     */
    fun text(
        content: String,
        style: TextStyle = TextStyle.NORMAL,
    ) {
        elements.add(MessageElement.Text(content, style))
    }

    /**
     * Add a heading to the message.
     */
    fun heading(
        content: String,
        level: Int = 1,
    ) {
        elements.add(MessageElement.Heading(content, level))
    }

    /**
     * Add an image to the message.
     */
    fun image(
        url: String,
        altText: String,
        placement: ImagePlacement = ImagePlacement.BLOCK,
    ) {
        elements.add(MessageElement.Image(url, altText, placement))
    }

    /**
     * Add a visual divider to the message.
     */
    fun divider() {
        elements.add(MessageElement.Divider)
    }

    /**
     * Add key-value fields to the message.
     */
    fun fields(vararg pairs: Pair<String, String>) {
        if (pairs.isNotEmpty()) {
            elements.add(
                MessageElement.KeyValueList(
                    pairs.map { KeyValue(it.first, it.second) },
                ),
            )
        }
    }

    /**
     * Add contextual information (small text) to the message.
     */
    fun context(content: String) {
        elements.add(MessageElement.Context(content))
    }

    /**
     * Build the final BotMessage.
     */
    internal fun build(style: MessageStyle? = null): BotMessage {
        return BotMessage(elements.toList(), style)
    }
}

/**
 * Build a platform-agnostic message using the MessageBuilder DSL.
 *
 * Example:
 * ```
 * val message = buildMessage {
 *     heading("Weather Forecast")
 *     text("It will be sunny today!")
 *     divider()
 *     fields(
 *         "Temperature" to "72°F",
 *         "Humidity" to "45%"
 *     )
 * }
 * ```
 */
fun buildMessage(
    style: MessageStyle? = null,
    block: MessageBuilder.() -> Unit,
): BotMessage {
    val builder = MessageBuilder()
    builder.block()
    return builder.build(style)
}

/**
 * Create a simple text-only message.
 * Convenience function for common use case.
 */
fun textMessage(content: String): BotMessage {
    return BotMessage(listOf(MessageElement.Text(content)))
}
