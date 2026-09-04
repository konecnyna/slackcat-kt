package com.slackcat.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlackLinkFormatterTest {
    @Test
    fun `toBareUrls unwraps an unlabeled Slack link`() {
        assertEquals("https://example.com", SlackLinkFormatter.toBareUrls("<https://example.com>"))
    }

    @Test
    fun `toBareUrls unwraps a labeled Slack link`() {
        assertEquals(
            "https://example.com/a.png",
            SlackLinkFormatter.toBareUrls("<https://example.com/a.png|a.png>"),
        )
    }

    @Test
    fun `toBareUrls unwraps a standard markdown link`() {
        assertEquals("https://example.com", SlackLinkFormatter.toBareUrls("[Example](https://example.com)"))
    }

    @Test
    fun `toBareUrls unwraps a standard markdown image`() {
        assertEquals("https://example.com/a.png", SlackLinkFormatter.toBareUrls("![alt](https://example.com/a.png)"))
    }

    @Test
    fun `toBareUrls unwraps a mailto link`() {
        assertEquals("mailto:a@b.com", SlackLinkFormatter.toBareUrls("<mailto:a@b.com|Email me>"))
    }

    @Test
    fun `toBareUrls preserves a user mention`() {
        assertEquals("hi <@U012AB3CD>", SlackLinkFormatter.toBareUrls("hi <@U012AB3CD>"))
    }

    @Test
    fun `toBareUrls preserves a labeled user mention`() {
        assertEquals("hi <@U012AB3CD|nick>", SlackLinkFormatter.toBareUrls("hi <@U012AB3CD|nick>"))
    }

    @Test
    fun `toBareUrls preserves a channel link`() {
        assertEquals("in <#C123ABC456|general>", SlackLinkFormatter.toBareUrls("in <#C123ABC456|general>"))
    }

    @Test
    fun `toBareUrls preserves special mentions`() {
        assertEquals(
            "<!here> <!channel> <!everyone> <!subteam^SAZ94GDB8>",
            SlackLinkFormatter.toBareUrls("<!here> <!channel> <!everyone> <!subteam^SAZ94GDB8>"),
        )
    }

    @Test
    fun `toBareUrls leaves plain text untouched`() {
        assertEquals("no links here", SlackLinkFormatter.toBareUrls("no links here"))
    }

    @Test
    fun `toSlackMrkdwn converts a standard markdown link`() {
        assertEquals(
            "read the <https://docs.slack.dev|docs>",
            SlackLinkFormatter.toSlackMrkdwn("read the [docs](https://docs.slack.dev)"),
        )
    }

    @Test
    fun `toSlackMrkdwn converts a standard markdown image to a link`() {
        assertEquals(
            "<https://example.com/a.png|alt>",
            SlackLinkFormatter.toSlackMrkdwn("![alt](https://example.com/a.png)"),
        )
    }

    @Test
    fun `toSlackMrkdwn uses the URL as the label when the markdown label is empty`() {
        assertEquals(
            "<https://example.com|https://example.com>",
            SlackLinkFormatter.toSlackMrkdwn("[](https://example.com)"),
        )
    }

    @Test
    fun `toSlackMrkdwn leaves an existing Slack link untouched`() {
        assertEquals(
            "<https://example.com|Example>",
            SlackLinkFormatter.toSlackMrkdwn("<https://example.com|Example>"),
        )
    }

    @Test
    fun `toSlackMrkdwn leaves mentions untouched`() {
        assertEquals("<@U012AB3CD> <!here>", SlackLinkFormatter.toSlackMrkdwn("<@U012AB3CD> <!here>"))
    }

    @Test
    fun `toSlackMrkdwn leaves a markdown link inside a code span untouched`() {
        assertEquals(
            "use `[label](url)` syntax",
            SlackLinkFormatter.toSlackMrkdwn("use `[label](url)` syntax"),
        )
    }

    @Test
    fun `toSlackLink builds mrkdwn link syntax`() {
        assertEquals("<https://example.com|Example>", SlackLinkFormatter.toSlackLink("https://example.com", "Example"))
    }

    @Test
    fun `toSlackLink falls back to the URL when no label is given`() {
        assertEquals(
            "<https://example.com|https://example.com>",
            SlackLinkFormatter.toSlackLink("https://example.com", null),
        )
    }

    @Test
    fun `extractFirstUrl finds the URL in a labeled Slack link`() {
        assertEquals(
            "https://example.com/a.png",
            SlackLinkFormatter.extractFirstUrl("look <https://example.com/a.png|a.png> here"),
        )
    }

    @Test
    fun `extractFirstUrl finds the URL in a standard markdown link`() {
        assertEquals(
            "https://example.com/a.png",
            SlackLinkFormatter.extractFirstUrl("look [a](https://example.com/a.png) here"),
        )
    }

    @Test
    fun `extractFirstUrl finds a bare URL`() {
        assertEquals("https://example.com/a.png", SlackLinkFormatter.extractFirstUrl("look https://example.com/a.png"))
    }

    @Test
    fun `extractFirstUrl returns null when there is no URL`() {
        assertEquals(null, SlackLinkFormatter.extractFirstUrl("hi <@U012AB3CD>"))
    }

    @Test
    fun `isImageUrl accepts common image extensions`() {
        assertTrue(SlackLinkFormatter.isImageUrl("https://example.com/a.PNG"))
        assertTrue(SlackLinkFormatter.isImageUrl("https://example.com/a.jpeg"))
        assertTrue(SlackLinkFormatter.isImageUrl("https://example.com/a.webp"))
    }

    @Test
    fun `isImageUrl accepts an image URL with a query string`() {
        assertTrue(SlackLinkFormatter.isImageUrl("https://example.com/a.png?width=200"))
    }

    @Test
    fun `isImageUrl rejects a non-image URL`() {
        assertFalse(SlackLinkFormatter.isImageUrl("https://example.com/page"))
    }

    @Test
    fun `isPrivateSlackFileUrl detects Slack hosted private files`() {
        assertTrue(SlackLinkFormatter.isPrivateSlackFileUrl("https://files.slack.com/files-pri/T1-F1/cat.png"))
        assertTrue(SlackLinkFormatter.isPrivateSlackFileUrl("https://myteam.slack.com/files/U1/F1/cat.png"))
    }

    @Test
    fun `isPrivateSlackFileUrl allows public Slack CDN assets`() {
        assertFalse(SlackLinkFormatter.isPrivateSlackFileUrl("https://emoji.slack-edge.com/T1/cat/abc.png"))
        assertFalse(SlackLinkFormatter.isPrivateSlackFileUrl("https://example.com/cat.png"))
    }
}
