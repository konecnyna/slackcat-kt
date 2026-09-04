package com.slackcat.chat.engine.slack

import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slackcat.common.ImagePlacement
import com.slackcat.common.buildMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlackMessageConverterTest {
    private val converter = SlackMessageConverter()

    private fun firstSectionText(text: String): String {
        val blocks = converter.toSlackBlocks(buildMessage { text(text) })
        val section = blocks.first() as SectionBlock
        return (section.text as MarkdownTextObject).text
    }

    @Test
    fun `text converts a standard markdown link to Slack mrkdwn`() {
        assertEquals(
            "read the <https://docs.slack.dev|docs>",
            firstSectionText("read the [docs](https://docs.slack.dev)"),
        )
    }

    @Test
    fun `text leaves an existing Slack link untouched`() {
        assertEquals("<https://example.com|Example>", firstSectionText("<https://example.com|Example>"))
    }

    @Test
    fun `text leaves mentions untouched`() {
        assertEquals(
            "<@U012AB3CD> in <#C123ABC456|general> <!here>",
            firstSectionText("<@U012AB3CD> in <#C123ABC456|general> <!here>"),
        )
    }

    @Test
    fun `context converts a standard markdown link to Slack mrkdwn`() {
        val blocks = converter.toSlackBlocks(buildMessage { context("via [source](https://example.com)") })
        val block = blocks.first() as ContextBlock
        val element = block.elements.first() as MarkdownTextObject
        assertEquals("via <https://example.com|source>", element.text)
    }

    @Test
    fun `link element renders as Slack mrkdwn link`() {
        assertEquals(
            "<https://example.com|Example>",
            firstSectionTextFrom(buildLinkMessage("https://example.com", "Example")),
        )
    }

    @Test
    fun `link element without a label falls back to the URL`() {
        assertEquals(
            "<https://example.com|https://example.com>",
            firstSectionTextFrom(buildLinkMessage("https://example.com", null)),
        )
    }

    @Test
    fun `fields convert a standard markdown link to Slack mrkdwn`() {
        val blocks = converter.toSlackBlocks(buildMessage { fields("Docs" to "[here](https://example.com)") })
        val section = blocks.first() as SectionBlock
        val field = section.fields.first() as MarkdownTextObject
        assertEquals("*Docs*\n<https://example.com|here>", field.text)
    }

    @Test
    fun `thumbnail image alt text is still rendered`() {
        val blocks =
            converter.toSlackBlocks(
                buildMessage { image("https://example.com/a.png", "alt", ImagePlacement.THUMBNAIL) },
            )
        val section = blocks.first() as SectionBlock
        assertEquals("alt", (section.text as MarkdownTextObject).text)
    }

    private fun buildLinkMessage(
        url: String,
        label: String?,
    ) = buildMessage { link(url, label) }

    private fun firstSectionTextFrom(message: com.slackcat.common.BotMessage): String {
        val section = converter.toSlackBlocks(message).first() as SectionBlock
        return (section.text as MarkdownTextObject).text
    }
}
