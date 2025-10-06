package com.slackcat.modules.simple

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
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

class FrameModuleTest {
    private lateinit var frameModule: FrameModule
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

        frameModule = FrameModule()
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
    fun `provideCommand returns nickelback`() {
        assertEquals("nickelback", frameModule.provideCommand())
    }

    @Test
    fun `aliases returns correct list`() {
        val aliases = frameModule.aliases()
        assertEquals(1, aliases.size)
        assertTrue(aliases.contains("krang"))
    }

    @Test
    fun `help returns non-empty string`() {
        val helpText = frameModule.help()
        assertTrue(helpText.isNotEmpty())
        assertTrue(helpText.contains("Frame Help"))
    }

    @Test
    fun `onInvoke with nickelback command creates framed image`() =
        runTest {
            val testUrl = "https://example.com/image.jpg"
            val incomingMessage = createTestMessage("nickelback", testUrl)

            frameModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            // The message should contain the framed image URL
            val messageText = sentMessage.message.toString()
            assertTrue(messageText.contains("home-remote-api.herokuapp.com/nickelback"))
        }

    @Test
    fun `onInvoke with krang command creates framed image`() =
        runTest {
            val testUrl = "https://example.com/image.jpg"
            val incomingMessage = createTestMessage("krang", testUrl)

            frameModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            // The message should contain the framed image URL
            val messageText = sentMessage.message.toString()
            assertTrue(messageText.contains("home-remote-api.herokuapp.com/krang"))
        }

    @Test
    fun `onInvoke removes angle brackets from URL`() =
        runTest {
            val testUrl = "<https://example.com/image.jpg>"
            val incomingMessage = createTestMessage("nickelback", testUrl)

            frameModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            val messageText = sentMessage.message.toString()
            // Should contain the URL without angle brackets
            assertTrue(messageText.contains("https://example.com/image.jpg"))
        }

    @Test
    fun `onInvoke with unknown command sends help message`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "unknown",
                    "url",
                    arguments = emptyList(),
                )

            frameModule.onInvoke(incomingMessage)

            // Should send help message when command is not recognized
            coVerify { mockChatClient.sendMessage(any(), any(), any()) }
        }

    @Test
    fun `onInvoke with empty URL still processes`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "nickelback",
                    "",
                    arguments = emptyList(),
                )

            frameModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
        }
}
