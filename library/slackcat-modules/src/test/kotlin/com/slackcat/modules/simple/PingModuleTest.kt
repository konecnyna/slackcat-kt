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

class PingModuleTest {
    private lateinit var pingModule: PingModule
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

        pingModule = PingModule()
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
    fun `provideCommand returns ping`() {
        assertEquals("ping", pingModule.provideCommand())
    }

    @Test
    fun `aliases returns correct list`() {
        val aliases = pingModule.aliases()
        assertEquals(3, aliases.size)
        assertTrue(aliases.contains("bing"))
        assertTrue(aliases.contains("ding"))
        assertTrue(aliases.contains("ring"))
    }

    @Test
    fun `help returns non-empty string`() {
        val helpMessage = pingModule.help()
        assertTrue(helpMessage.elements.isNotEmpty())
        // Check that help message contains heading or text with the expected content
        val hasExpectedContent =
            helpMessage.elements.any { element ->
                when (element) {
                    is MessageElement.Heading -> element.content.contains("Ping Help")
                    is MessageElement.Text -> element.content.contains("Ping Help")
                    else -> false
                }
            }
        assertTrue(hasExpectedContent)
    }

    @Test
    fun `onInvoke with ping command sends pong`() =
        runTest {
            val incomingMessage = createTestMessage("ping")

            pingModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            assertTrue(sentMessage.message.toString().contains("pong"))
        }

    @Test
    fun `onInvoke with bing command sends bong`() =
        runTest {
            val incomingMessage = createTestMessage("bing")

            pingModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertTrue(sentMessage.message.toString().contains("bong"))
        }

    @Test
    fun `onInvoke with ding command sends dong`() =
        runTest {
            val incomingMessage = createTestMessage("ding")

            pingModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertTrue(sentMessage.message.toString().contains("dong"))
        }

    @Test
    fun `onInvoke with ring command sends wrong`() =
        runTest {
            val incomingMessage = createTestMessage("ring")

            pingModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertTrue(sentMessage.message.toString().contains("wrong"))
        }

    @Test
    fun `onInvoke with unknown command defaults to pong`() =
        runTest {
            val incomingMessage = createTestMessage("unknown")

            pingModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertTrue(sentMessage.message.toString().contains("pong"))
        }
}
