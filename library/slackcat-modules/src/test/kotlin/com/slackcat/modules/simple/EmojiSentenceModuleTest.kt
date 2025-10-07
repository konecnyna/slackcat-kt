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

class EmojiSentenceModuleTest {
    private lateinit var emojiSentenceModule: EmojiSentenceModule
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

        emojiSentenceModule = EmojiSentenceModule()
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
    fun `provideCommand returns emojisetence`() {
        // Note: typo in the actual implementation
        assertEquals("emojisetence", emojiSentenceModule.provideCommand())
    }

    @Test
    fun `aliases returns empty list`() {
        val aliases = emojiSentenceModule.aliases()
        assertTrue(aliases.isEmpty())
    }

    @Test
    fun `help returns non-empty string`() {
        val helpMessage = emojiSentenceModule.help()
        assertTrue(helpMessage.elements.isNotEmpty())
        // Check that help message contains heading or text with the expected content
        val hasExpectedContent =
            helpMessage.elements.any { element ->
                when (element) {
                    is MessageElement.Heading -> element.content.contains("Emojisentence Help")
                    is MessageElement.Text -> element.content.contains("Emojisentence Help")
                    else -> false
                }
            }
        assertTrue(hasExpectedContent)
    }

    @Test
    fun `onInvoke converts text to emoji format`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "emojisetence",
                    "hello",
                    arguments = emptyList(),
                )

            emojiSentenceModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)

            // Should contain emoji alphabet format
            val hasEmojiAlphabet =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains(":alphabet-")
                        is MessageElement.Heading -> element.content.contains(":alphabet-")
                        else -> false
                    }
                }
            assertTrue(hasEmojiAlphabet)
        }

    @Test
    fun `onInvoke converts lowercase letters to emoji`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "emojisetence",
                    "a",
                    arguments = emptyList(),
                )

            emojiSentenceModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should contain alphabet emoji
            val hasAlphabet =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains(":alphabet-")
                        is MessageElement.Heading -> element.content.contains(":alphabet-")
                        else -> false
                    }
                }
            assertTrue(hasAlphabet)

            val hasLetterA =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("-a:")
                        is MessageElement.Heading -> element.content.contains("-a:")
                        else -> false
                    }
                }
            assertTrue(hasLetterA)
        }

    @Test
    fun `onInvoke converts uppercase to lowercase emoji`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "emojisetence",
                    "ABC",
                    arguments = emptyList(),
                )

            emojiSentenceModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should convert to lowercase and contain emoji format
            val hasAlphabet =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains(":alphabet-")
                        is MessageElement.Heading -> element.content.contains(":alphabet-")
                        else -> false
                    }
                }
            assertTrue(hasAlphabet)

            val hasLetterA =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("-a:")
                        is MessageElement.Heading -> element.content.contains("-a:")
                        else -> false
                    }
                }
            assertTrue(hasLetterA)

            val hasLetterB =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("-b:")
                        is MessageElement.Heading -> element.content.contains("-b:")
                        else -> false
                    }
                }
            assertTrue(hasLetterB)

            val hasLetterC =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("-c:")
                        is MessageElement.Heading -> element.content.contains("-c:")
                        else -> false
                    }
                }
            assertTrue(hasLetterC)
        }

    @Test
    fun `onInvoke preserves non-alphabetic characters`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "emojisetence",
                    "a b",
                    arguments = emptyList(),
                )

            emojiSentenceModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should preserve the space between letters
            val hasAlphabet =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains(":alphabet-")
                        is MessageElement.Heading -> element.content.contains(":alphabet-")
                        else -> false
                    }
                }
            assertTrue(hasAlphabet)

            // The space should be preserved
            val hasSpace =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains(" ")
                        is MessageElement.Heading -> element.content.contains(" ")
                        else -> false
                    }
                }
            assertTrue(hasSpace)
        }

    @Test
    fun `onInvoke handles empty text`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "emojisetence",
                    "",
                    arguments = emptyList(),
                )

            emojiSentenceModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
        }

    @Test
    fun `onInvoke handles numbers and special characters`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    "emojisetence",
                    "a1!",
                    arguments = emptyList(),
                )

            emojiSentenceModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            // Should convert 'a' to emoji but preserve '1' and '!'
            val hasAlphabet =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains(":alphabet-")
                        is MessageElement.Heading -> element.content.contains(":alphabet-")
                        else -> false
                    }
                }
            assertTrue(hasAlphabet)

            val hasLetterA =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("-a:")
                        is MessageElement.Heading -> element.content.contains("-a:")
                        else -> false
                    }
                }
            assertTrue(hasLetterA)
        }
}
