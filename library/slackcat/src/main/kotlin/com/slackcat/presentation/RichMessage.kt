package com.slackcat.presentation

import com.slackcat.common.RichTextMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RichTextMessageBuilder {
    private val blocks = mutableListOf<Block>()

    fun divider() {
        blocks.add(Block.Divider)
    }

    fun section(
        text: String,
        imageUrl: String? = null,
        altText: String? = null,
    ) {
        val section =
            Block.Section(
                text = TextObject(text = text, type = "mrkdwn"),
                accessory = imageUrl?.let { Accessory(type = "image", image_url = it, alt_text = altText ?: "") },
            )
        blocks.add(section)
    }

    fun text(text: String) {
        val section =
            Block.Section(
                text = TextObject(text = text, type = "mrkdwn"),
            )
        blocks.add(section)
    }

    fun context(text: String) {
        val section =
            Block.Context(
                listOf(TextObject(text = text, type = "mrkdwn")),
            )
        blocks.add(section)
    }

    fun image(
        imageUrl: String,
        altText: String,
    ) {
        blocks.add(Block.Image(image_url = imageUrl, alt_text = altText))
    }

    fun build(): String {
        return Json.encodeToString(RichMessage(blocks))
    }
}

inline fun buildRichMessage(builderAction: RichTextMessageBuilder.() -> Unit): RichTextMessage {
    val builder = RichTextMessageBuilder()
    builder.builderAction()
    return RichTextMessage(builder.build())
}

inline fun text(value: String) = buildRichMessage { section(value) }

inline fun buildAttachment(
    color: String,
    builderAction: RichTextMessageBuilder.() -> Unit,
): com.slackcat.common.MessageAttachment {
    val builder = RichTextMessageBuilder()
    builder.builderAction()
    return com.slackcat.common.MessageAttachment(
        color = color,
        blocks = builder.build(),
    )
}

fun messageWithAttachment(
    color: String,
    builderAction: RichTextMessageBuilder.() -> Unit,
): com.slackcat.common.RichTextMessage {
    val attachment = buildAttachment(color, builderAction)
    return com.slackcat.common.RichTextMessage(
        text = "",
        attachments = listOf(attachment),
    )
}

// Define a structure to represent blocks
@Serializable
data class RichMessage(val blocks: List<Block>)

@Serializable
sealed class Block {
    @Serializable
    @SerialName("divider")
    data object Divider : Block()

    @Serializable
    @SerialName("section")
    data class Section(
        val text: TextObject,
        val accessory: Accessory? = null,
    ) : Block()

    @Serializable
    @SerialName("image")
    data class Image(
        val image_url: String,
        val alt_text: String,
    ) : Block()

    @Serializable
    @SerialName("caption")
    data class Context(
        val elements: List<TextObject>,
    ) : Block()
}

@Serializable
data class TextObject(val type: String, val text: String)

@Serializable
data class Accessory(val type: String, val image_url: String, val alt_text: String)
