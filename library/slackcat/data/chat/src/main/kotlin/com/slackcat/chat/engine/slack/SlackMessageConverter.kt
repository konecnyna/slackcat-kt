package com.slackcat.chat.engine.slack

import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.ImageBlock
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.element.ImageElement
import com.slackcat.common.BotMessage
import com.slackcat.common.MessageElement
import com.slackcat.common.MessageStyle
import com.slackcat.common.TextStyle

/**
 * Converts platform-agnostic BotMessage to Slack Block Kit format.
 */
class SlackMessageConverter {
    fun toSlackBlocks(message: BotMessage): List<LayoutBlock> {
        return message.elements.mapNotNull { element ->
            when (element) {
                is MessageElement.Text -> convertText(element)
                is MessageElement.Heading -> convertHeading(element)
                is MessageElement.Image -> convertImage(element)
                is MessageElement.Divider -> DividerBlock.builder().build()
                is MessageElement.KeyValueList -> convertKeyValueList(element)
                is MessageElement.Context -> convertContext(element)
            }
        }
    }

    private fun convertText(text: MessageElement.Text): SectionBlock {
        val formattedText =
            when (text.style) {
                TextStyle.BOLD -> "*${text.content}*"
                TextStyle.CODE -> "`${text.content}`"
                TextStyle.QUOTE -> ">${text.content}"
                TextStyle.NORMAL -> text.content
            }

        return SectionBlock.builder()
            .text(MarkdownTextObject(formattedText, true))
            .build()
    }

    private fun convertHeading(heading: MessageElement.Heading): SectionBlock {
        // Slack doesn't have true headings, so use bold text
        return SectionBlock.builder()
            .text(MarkdownTextObject("*${heading.content}*", true))
            .build()
    }

    private fun convertImage(image: MessageElement.Image): LayoutBlock? {
        return when (image.placement) {
            com.slackcat.common.ImagePlacement.BLOCK -> {
                ImageBlock.builder()
                    .imageUrl(image.url)
                    .altText(image.altText)
                    .build()
            }
            com.slackcat.common.ImagePlacement.THUMBNAIL -> {
                // Thumbnail images need to be attached to a section block as accessories
                // For now, treat them as regular sections with image accessories
                SectionBlock.builder()
                    .text(MarkdownTextObject(image.altText, true))
                    .accessory(
                        ImageElement.builder()
                            .imageUrl(image.url)
                            .altText(image.altText)
                            .build(),
                    )
                    .build()
            }
        }
    }

    private fun convertKeyValueList(keyValueList: MessageElement.KeyValueList): SectionBlock {
        val fields =
            keyValueList.items.map { item ->
                MarkdownTextObject("*${item.key}*\n${item.value}", true)
            }

        return SectionBlock.builder()
            .fields(fields)
            .build()
    }

    private fun convertContext(context: MessageElement.Context): ContextBlock {
        return ContextBlock.builder()
            .elements(listOf(MarkdownTextObject(context.content, true)))
            .build()
    }

    /**
     * Converts MessageStyle to Slack attachment color string.
     */
    fun toColorString(style: MessageStyle?): String? {
        return when (style) {
            MessageStyle.SUCCESS -> "good"
            MessageStyle.WARNING -> "warning"
            MessageStyle.ERROR -> "danger"
            MessageStyle.INFO -> "#2196F3"
            MessageStyle.NEUTRAL -> null
            null -> null
        }
    }
}
