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
    companion object {
        // Slack's limit is 3000, use 2900 to leave buffer for formatting characters
        private const val MAX_BLOCK_TEXT_LENGTH = 2900
    }

    fun toSlackBlocks(message: BotMessage): List<LayoutBlock> {
        return message.elements.flatMap { element ->
            when (element) {
                is MessageElement.Text -> convertText(element)
                is MessageElement.Heading -> listOf(convertHeading(element))
                is MessageElement.Image -> listOfNotNull(convertImage(element))
                is MessageElement.Divider -> listOf(DividerBlock.builder().build())
                is MessageElement.KeyValueList -> listOf(convertKeyValueList(element))
                is MessageElement.Context -> convertContext(element)
            }
        }
    }

    private fun convertText(text: MessageElement.Text): List<SectionBlock> {
        val formattedText =
            when (text.style) {
                TextStyle.BOLD -> "*${text.content}*"
                TextStyle.CODE -> "`${text.content}`"
                TextStyle.QUOTE -> ">${text.content}"
                TextStyle.NORMAL -> text.content
            }

        return chunkText(formattedText).map { chunk ->
            SectionBlock.builder()
                .text(MarkdownTextObject(chunk, true))
                .build()
        }
    }

    /**
     * Intelligently splits text into chunks that fit within Slack's character limit.
     * Preserves formatting by splitting on paragraph breaks, then line breaks, then word boundaries.
     */
    private fun chunkText(text: String): List<String> {
        if (text.length <= MAX_BLOCK_TEXT_LENGTH) {
            return listOf(text)
        }

        val chunks = mutableListOf<String>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            if (remaining.length <= MAX_BLOCK_TEXT_LENGTH) {
                chunks.add(remaining)
                break
            }

            // Find the best split point within the limit
            val splitPoint = findBestSplitPoint(remaining, MAX_BLOCK_TEXT_LENGTH)
            chunks.add(remaining.substring(0, splitPoint).trimEnd())
            remaining = remaining.substring(splitPoint).trimStart()
        }

        return chunks
    }

    /**
     * Finds the best point to split text, preferring:
     * 1. Paragraph breaks (double newline)
     * 2. Line breaks (single newline)
     * 3. Word boundaries (space)
     * 4. Hard cut at limit (last resort)
     */
    private fun findBestSplitPoint(
        text: String,
        maxLength: Int,
    ): Int {
        val searchRange = text.substring(0, maxLength)

        // Try to split at paragraph break (double newline)
        val paragraphBreak = searchRange.lastIndexOf("\n\n")
        if (paragraphBreak > maxLength / 2) {
            return paragraphBreak + 2
        }

        // Try to split at line break
        val lineBreak = searchRange.lastIndexOf('\n')
        if (lineBreak > maxLength / 2) {
            return lineBreak + 1
        }

        // Try to split at word boundary (space)
        val wordBreak = searchRange.lastIndexOf(' ')
        if (wordBreak > maxLength / 2) {
            return wordBreak + 1
        }

        // Hard cut at limit
        return maxLength
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

    private fun convertContext(context: MessageElement.Context): List<ContextBlock> {
        return chunkText(context.content).map { chunk ->
            ContextBlock.builder()
                .elements(listOf(MarkdownTextObject(chunk, true)))
                .build()
        }
    }

    /**
     * Converts MessageStyle to Slack attachment color string.
     */
    fun toColorString(style: MessageStyle?): String? {
        return when (style) {
            MessageStyle.SUCCESS -> "#4CAF50"
            MessageStyle.WARNING -> "#FFC107"
            MessageStyle.ERROR -> "#F44336"
            MessageStyle.INFO -> "#2196F3"
            MessageStyle.NEUTRAL -> null
            is MessageStyle.Custom -> style.hexColor
            null -> null
        }
    }
}
