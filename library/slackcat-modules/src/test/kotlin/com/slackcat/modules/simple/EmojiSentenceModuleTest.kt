package com.slackcat.modules.simple

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmojiSentenceModuleTest {
    private lateinit var emojiSentenceModule: EmojiSentenceModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope

    @BeforeEach
    fun setup() {
        emojiSentenceModule = EmojiSentenceModule()
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)

        emojiSentenceModule.chatClient = mockChatClient
        emojiSentenceModule.coroutineScope = mockCoroutineScope

        coEvery { mockChatClient.sendMessage(any()) } returns Result.success(Unit)
    }
    private fun createTestMessage(
        command: String,
        userText: String = "",
        channelId: String = "channel123",
        arguments: List<String> = emptyList()
    ) = IncomingChatMessage(
        arguments = arguments,
        command = command,
        channelId = channelId,
        chatUser = ChatUser("user123"),
        messageId = "msg123",
        rawMessage = "?$command $userText",
        userText = userText
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
        val helpText = emojiSentenceModule.help()
        assertTrue(helpText.isNotEmpty())
        assertTrue(helpText.contains("Emojisentence Help"))
    }

    @Test
    fun `onInvoke converts text to emoji format`() = runTest {
        val incomingMessage = createTestMessage("emojisetence", "hello", arguments = emptyList()
        )

        emojiSentenceModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        assertEquals("channel123", sentMessage.channelId)

        val messageText = sentMessage.message.toString()
        // Should contain emoji alphabet format
        assertTrue(messageText.contains(":alphabet-"))
    }

    @Test
    fun `onInvoke converts lowercase letters to emoji`() = runTest {
        val incomingMessage = createTestMessage("emojisetence", "a", arguments = emptyList()
        )

        emojiSentenceModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should contain alphabet emoji
        assertTrue(messageText.contains(":alphabet-"))
        assertTrue(messageText.contains("-a:"))
    }

    @Test
    fun `onInvoke converts uppercase to lowercase emoji`() = runTest {
        val incomingMessage = createTestMessage("emojisetence", "ABC", arguments = emptyList()
        )

        emojiSentenceModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should convert to lowercase and contain emoji format
        assertTrue(messageText.contains(":alphabet-"))
        assertTrue(messageText.contains("-a:"))
        assertTrue(messageText.contains("-b:"))
        assertTrue(messageText.contains("-c:"))
    }

    @Test
    fun `onInvoke preserves non-alphabetic characters`() = runTest {
        val incomingMessage = createTestMessage("emojisetence", "a b", arguments = emptyList()
        )

        emojiSentenceModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should preserve the space between letters
        assertTrue(messageText.contains(":alphabet-"))
        // The space should be preserved
        assertTrue(messageText.contains(" "))
    }

    @Test
    fun `onInvoke handles empty text`() = runTest {
        val incomingMessage = createTestMessage("emojisetence", "", arguments = emptyList()
        )

        emojiSentenceModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        assertEquals("channel123", sentMessage.channelId)
    }

    @Test
    fun `onInvoke handles numbers and special characters`() = runTest {
        val incomingMessage = createTestMessage("emojisetence", "a1!", arguments = emptyList()
        )

        emojiSentenceModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should convert 'a' to emoji but preserve '1' and '!'
        assertTrue(messageText.contains(":alphabet-"))
        assertTrue(messageText.contains("-a:"))
    }
}
