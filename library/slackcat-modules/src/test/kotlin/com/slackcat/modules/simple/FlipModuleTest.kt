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

class FlipModuleTest {
    private lateinit var flipModule: FlipModule
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

        flipModule = FlipModule()
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
    fun `provideCommand returns flip`() {
        assertEquals("flip", flipModule.provideCommand())
    }

    @Test
    fun `aliases returns empty list`() {
        val aliases = flipModule.aliases()
        assertTrue(aliases.isEmpty())
    }

    @Test
    fun `help returns non-empty string`() {
        val helpMessage = flipModule.help()
        assertTrue(helpMessage.elements.isNotEmpty())
        // Check that help message contains heading or text with the expected content
        val hasExpectedContent =
            helpMessage.elements.any { element ->
                when (element) {
                    is MessageElement.Heading -> element.content.contains("Flip Help")
                    is MessageElement.Text -> element.content.contains("Flip Help")
                    else -> false
                }
            }
        assertTrue(hasExpectedContent)
    }

    @Test
    fun `onInvoke with empty text sends error message`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "flip",
                    "",
                    arguments = emptyList(),
                )

            flipModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)

            val hasErrorMessage =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Please provide text to flip")
                        is MessageElement.Heading -> element.content.contains("Please provide text to flip")
                        else -> false
                    }
                }
            assertTrue(hasErrorMessage)
        }

    @Test
    fun `onInvoke with text flips it correctly`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "flip",
                    "hello",
                    arguments = emptyList(),
                )

            flipModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should contain the table flip emoticon
            val hasTableFlip =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("(╯°□°）╯︵")
                        is MessageElement.Heading -> element.content.contains("(╯°□°）╯︵")
                        else -> false
                    }
                }
            assertTrue(hasTableFlip)

            // Should contain flipped text (ollǝɥ is hello flipped)
            val hasFlippedText =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("ollǝɥ")
                        is MessageElement.Heading -> element.content.contains("ollǝɥ")
                        else -> false
                    }
                }
            assertTrue(hasFlippedText)
        }

    @Test
    fun `onInvoke flips uppercase letters`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "flip",
                    "HELLO",
                    arguments = emptyList(),
                )

            flipModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should contain flipped text
            val hasTableFlip =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("(╯°□°）╯︵")
                        is MessageElement.Heading -> element.content.contains("(╯°□°）╯︵")
                        else -> false
                    }
                }
            assertTrue(hasTableFlip)
        }

    @Test
    fun `onInvoke flips numbers correctly`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "flip",
                    "123",
                    arguments = emptyList(),
                )

            flipModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should contain the table flip emoticon and flipped numbers
            val hasTableFlip =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("(╯°□°）╯︵")
                        is MessageElement.Heading -> element.content.contains("(╯°□°）╯︵")
                        else -> false
                    }
                }
            assertTrue(hasTableFlip)

            // 123 flipped should be ƐᄅƖ
            val hasFlippedNumbers =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("ƐᄅƖ")
                        is MessageElement.Heading -> element.content.contains("ƐᄅƖ")
                        else -> false
                    }
                }
            assertTrue(hasFlippedNumbers)
        }

    @Test
    fun `onInvoke flips punctuation correctly`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "flip",
                    "hello!",
                    arguments = emptyList(),
                )

            flipModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should contain flipped text
            val hasTableFlip =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("(╯°□°）╯︵")
                        is MessageElement.Heading -> element.content.contains("(╯°□°）╯︵")
                        else -> false
                    }
                }
            assertTrue(hasTableFlip)

            // ! flipped is ¡
            val hasFlippedExclamation =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("¡")
                        is MessageElement.Heading -> element.content.contains("¡")
                        else -> false
                    }
                }
            assertTrue(hasFlippedExclamation)
        }

    @Test
    fun `onInvoke preserves unflippable characters`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "flip",
                    "@#$",
                    arguments = emptyList(),
                )

            flipModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should contain the table flip emoticon
            val hasTableFlip =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("(╯°□°）╯︵")
                        is MessageElement.Heading -> element.content.contains("(╯°□°）╯︵")
                        else -> false
                    }
                }
            assertTrue(hasTableFlip)
        }
}
