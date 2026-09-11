package com.slackcat.chat.engine.slack

import com.slack.api.model.block.RichTextBlock
import com.slack.api.model.block.element.RichTextSectionElement
import com.slack.api.model.block.element.RichTextUnknownElement
import com.slack.api.model.event.MessageEvent
import com.slack.api.util.json.GsonFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins the Slack payload shape the ?learn image fix depends on. Slack omits `files` from a message
 * event when it renders a hosted file inline, and Bolt discards the file id with it.
 */
class InlineFileBlockTest {
    private val inlineFileEvent =
        """
        {
          "type": "message",
          "user": "U068323GTC7",
          "ts": "1789158272.882369",
          "text": "?learn imgonnasayit F0C13M1K7DH",
          "blocks": [
            {
              "type": "rich_text",
              "block_id": "osxoz",
              "elements": [
                {
                  "type": "rich_text_section",
                  "elements": [
                    { "type": "text", "text": "?learn imgonnasayit " },
                    {
                      "type": "file",
                      "file_id": "F0C13M1K7DH",
                      "url": "https://team.slack.com/files/U1/F0C13M1K7DH/image.png"
                    }
                  ]
                }
              ]
            }
          ],
          "channel": "C0A2XH1JWUW",
          "event_ts": "1789158272.882369",
          "channel_type": "group"
        }
        """.trimIndent()

    private fun parse(json: String): MessageEvent =
        GsonFactory.createSnakeCase().fromJson(
            json,
            MessageEvent::class.java,
        )

    private fun MessageEvent.fileElements() =
        blocks.orEmpty()
            .filterIsInstance<RichTextBlock>()
            .flatMap { it.elements.orEmpty() }
            .filterIsInstance<RichTextSectionElement>()
            .flatMap { it.elements.orEmpty() }
            .filterIsInstance<RichTextUnknownElement>()
            .filter { it.type == "file" }

    @Test
    fun `an inline hosted file leaves the event files array empty`() {
        assertNull(parse(inlineFileEvent).files)
    }

    @Test
    fun `an inline hosted file leaves its bare file id in the message text`() {
        assertEquals("?learn imgonnasayit F0C13M1K7DH", parse(inlineFileEvent).text)
    }

    @Test
    fun `an inline hosted file is detectable as an unknown rich text element`() {
        assertEquals(1, parse(inlineFileEvent).fileElements().size)
    }

    @Test
    fun `a message with no attachment has no file element`() {
        val plain =
            """
            {
              "type": "message",
              "user": "U1",
              "ts": "1.1",
              "text": "?learn key value",
              "blocks": [
                {
                  "type": "rich_text",
                  "elements": [
                    { "type": "rich_text_section", "elements": [{ "type": "text", "text": "?learn key value" }] }
                  ]
                }
              ],
              "channel": "C1"
            }
            """.trimIndent()

        assertTrue(parse(plain).fileElements().isEmpty())
    }
}
