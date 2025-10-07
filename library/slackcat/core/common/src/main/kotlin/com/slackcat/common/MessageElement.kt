package com.slackcat.common

/**
 * Platform-agnostic message element that can be rendered by any chat platform.
 * Each ChatEngine implementation converts these to platform-specific formats.
 */
sealed interface MessageElement {
    /**
     * Plain text content with optional styling.
     */
    data class Text(
        val content: String,
        val style: TextStyle = TextStyle.NORMAL,
    ) : MessageElement

    /**
     * Heading text (rendered as bold/emphasized across platforms).
     */
    data class Heading(
        val content: String,
        val level: Int = 1,
    ) : MessageElement

    /**
     * Image element with URL and alt text.
     */
    data class Image(
        val url: String,
        val altText: String,
        val placement: ImagePlacement = ImagePlacement.BLOCK,
    ) : MessageElement

    /**
     * Visual divider/separator.
     */
    data object Divider : MessageElement

    /**
     * Structured key-value pairs (useful for displaying data in fields).
     */
    data class KeyValueList(
        val items: List<KeyValue>,
    ) : MessageElement

    /**
     * Contextual information (rendered as small text/metadata across platforms).
     */
    data class Context(
        val content: String,
    ) : MessageElement
}

enum class TextStyle {
    NORMAL,
    BOLD,
    CODE,
    QUOTE,
}

enum class ImagePlacement {
    BLOCK, // Full-width image block
    THUMBNAIL, // Small thumbnail (as accessory/sidebar)
}

enum class MessageStyle {
    INFO,
    WARNING,
    ERROR,
    SUCCESS,
    NEUTRAL,
}

data class KeyValue(
    val key: String,
    val value: String,
)
