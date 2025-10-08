package com.slackcat.common

/**
 * Represents capabilities that different chat platforms may or may not support.
 * Modules can query these capabilities to adapt their behavior.
 */
enum class ChatCapability {
    /** Platform supports rich text formatting (bold, italic, code blocks, etc.) */
    RICH_FORMATTING,

    /** Platform can display images */
    IMAGES,

    /** Platform supports thumbnail/accessory images alongside text */
    THUMBNAILS,

    /** Platform supports visual dividers/separators */
    DIVIDERS,

    /** Platform supports structured key-value fields */
    STRUCTURED_FIELDS,

    /** Platform supports threaded conversations */
    THREADS,

    /** Platform supports emoji reactions on messages */
    REACTIONS,

    /** Platform allows customizing the bot's icon per message */
    CUSTOM_BOT_ICON,

    /** Platform allows customizing the bot's display name per message */
    CUSTOM_BOT_NAME,

    /** Platform supports colored message styling (info, warning, error, etc.) */
    MESSAGE_COLORS,
}
