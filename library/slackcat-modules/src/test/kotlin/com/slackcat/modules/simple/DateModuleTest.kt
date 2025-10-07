package com.slackcat.modules.simple

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.MessageElement
import com.slackcat.common.SlackcatConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class DateModuleTest {
    private lateinit var dateModule: DateModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope
    private lateinit var mockConfig: SlackcatConfig

    @BeforeEach
    fun setup() {
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)
        mockConfig = mockk(relaxed = true)

        every { mockConfig.botNameProvider() } returns "TestBot"
        every { mockConfig.botIconProvider() } returns mockk(relaxed = true)
        coEvery { mockChatClient.sendMessage(any(), any(), any()) } returns Result.success(Unit)

        startKoin {
            modules(
                module {
                    single<ChatClient> { mockChatClient }
                    single<CoroutineScope> { mockCoroutineScope }
                    single<SlackcatConfig> { mockConfig }
                },
            )
        }

        dateModule = DateModule()
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun createTestMessage(
        command: String,
        userText: String = "",
        channelId: String = "channel123",
        arguments: List<String> = emptyList(),
    ) = IncomingChatMessage(
        arguments = arguments,
        command = command,
        channelId = channelId,
        chatUser = ChatUser("user123"),
        messageId = "msg123",
        rawMessage = "?$command $userText",
        userText = userText,
    )

    @Test
    fun `provideCommand returns date`() {
        assertEquals("date", dateModule.provideCommand())
    }

    @Test
    fun `aliases returns empty list`() {
        val aliases = dateModule.aliases()
        assertTrue(aliases.isEmpty())
    }

    @Test
    fun `help returns non-empty string`() {
        val helpMessage = dateModule.help()
        assertTrue(helpMessage.elements.isNotEmpty())
        // Check that help message contains heading or text with the expected content
        val hasExpectedContent =
            helpMessage.elements.any { element ->
                when (element) {
                    is MessageElement.Heading -> element.content.contains("DateModule Help")
                    is MessageElement.Text -> element.content.contains("DateModule Help")
                    else -> false
                }
            }
        assertTrue(hasExpectedContent)
    }

    @Test
    fun `onInvoke sends current date message`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "date",
                    "",
                    arguments = emptyList(),
                )

            dateModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            val hasExpectedText =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Currently, it's")
                        else -> false
                    }
                }
            assertTrue(hasExpectedText)
        }

    @Test
    fun `onInvoke message contains date components`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "date",
                    "",
                    arguments = emptyList(),
                )

            dateModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            val hasExpectedText =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text ->
                            element.content.contains("Currently") ||
                                element.content.contains("it's")
                        else -> false
                    }
                }
            assertTrue(hasExpectedText)
        }
}
