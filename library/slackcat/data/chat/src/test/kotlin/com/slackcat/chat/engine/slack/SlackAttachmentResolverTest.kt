package com.slackcat.chat.engine.slack

import com.slack.api.model.File
import com.slack.api.model.event.MessageEvent
import com.slack.api.util.json.GsonFactory
import com.slackcat.chat.models.ChatAttachment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SlackAttachmentResolverTest {
    private lateinit var apiOps: SlackApiOperations
    private lateinit var resolver: SlackAttachmentResolver

    @BeforeEach
    fun setup() {
        apiOps = mockk()
        resolver = SlackAttachmentResolver(apiOps)
    }

    private fun parse(json: String): MessageEvent =
        GsonFactory.createSnakeCase().fromJson(
            json,
            MessageEvent::class.java,
        )

    private fun attachment(id: String = "F0C13M1K7DH") = ChatAttachment(id, "image.png", "image/png", "", "")

    private fun slackFile(
        id: String = "F0C13M1K7DH",
        mimetype: String = "image/png",
    ) = File().apply {
        this.id = id
        this.name = "image.png"
        this.mimetype = mimetype
        this.urlPrivate = "https://files.slack.com/files-pri/T1-$id/image.png"
        this.permalink = "https://team.slack.com/files/U1/$id/image.png"
    }

    private fun inlineFileEvent(threadTs: String? = null) =
        parse(
            """
            {
              "type": "message",
              "user": "U1",
              "ts": "1789158272.882369",
              ${threadTs?.let { "\"thread_ts\": \"$it\"," } ?: ""}
              "text": "?learn imgonnasayit F0C13M1K7DH",
              "blocks": [
                {
                  "type": "rich_text",
                  "elements": [
                    {
                      "type": "rich_text_section",
                      "elements": [
                        { "type": "text", "text": "?learn imgonnasayit " },
                        { "type": "file", "file_id": "F0C13M1K7DH", "url": "https://team.slack.com/files/U1/F0C13M1K7DH/image.png" }
                      ]
                    }
                  ]
                }
              ],
              "channel": "C1"
            }
            """.trimIndent(),
        )

    private val plainEvent =
        parse(
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
            """.trimIndent(),
        )

    @Test
    fun `resolve reads the file from the API when the event omits it`() =
        runTest {
            coEvery { apiOps.getMessageFiles("C1", "1789158272.882369", null) } returns
                Result.success(listOf(slackFile()))

            val attachments = resolver.resolve(inlineFileEvent())

            assertEquals(1, attachments.size)
            assertEquals("F0C13M1K7DH", attachments.first().id)
            assertTrue(attachments.first().isImage)
        }

    @Test
    fun `resolve passes the thread timestamp through to the API`() =
        runTest {
            coEvery { apiOps.getMessageFiles(any(), any(), any()) } returns Result.success(listOf(slackFile()))

            resolver.resolve(inlineFileEvent(threadTs = "1789157814.503719"))

            coVerify { apiOps.getMessageFiles("C1", "1789158272.882369", "1789157814.503719") }
        }

    @Test
    fun `resolve skips the API when the message carries no file element`() =
        runTest {
            assertEquals(emptyList<Any>(), resolver.resolve(plainEvent))

            coVerify(exactly = 0) { apiOps.getMessageFiles(any(), any(), any()) }
        }

    @Test
    fun `resolve returns no attachment when the API call fails`() =
        runTest {
            coEvery { apiOps.getMessageFiles(any(), any(), any()) } returns
                Result.failure(Exception("Slack API error: missing_scope"))

            assertEquals(emptyList<Any>(), resolver.resolve(inlineFileEvent()))
        }

    @Test
    fun `resolve drops a file with no id`() =
        runTest {
            coEvery { apiOps.getMessageFiles(any(), any(), any()) } returns
                Result.success(listOf(File(), slackFile()))

            assertEquals(listOf("F0C13M1K7DH"), resolver.resolve(inlineFileEvent()).map { it.id })
        }

    @Test
    fun `stripAttachmentTokens removes the bare file id Slack left in the text`() {
        assertEquals(
            "?learn imgonnasayit",
            resolver.stripAttachmentTokens("?learn imgonnasayit F0C13M1K7DH", listOf(attachment())),
        )
    }

    @Test
    fun `stripAttachmentTokens keeps a longer token that merely starts with the id`() {
        assertEquals(
            "?learn key F0C13M1K7DHX",
            resolver.stripAttachmentTokens(
                "?learn key F0C13M1K7DHX",
                listOf(attachment()),
            ),
        )
    }

    @Test
    fun `stripAttachmentTokens leaves the text alone when there is no attachment`() {
        assertEquals("?learn key value", resolver.stripAttachmentTokens("?learn key value", emptyList()))
    }
}
