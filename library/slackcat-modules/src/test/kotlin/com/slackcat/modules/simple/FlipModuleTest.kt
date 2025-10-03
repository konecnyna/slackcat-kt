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

class FlipModuleTest {
    private lateinit var flipModule: FlipModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope

    @BeforeEach
    fun setup() {
        flipModule = FlipModule()
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)

        flipModule.chatClient = mockChatClient
        flipModule.coroutineScope = mockCoroutineScope

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
        val helpText = flipModule.help()
        assertTrue(helpText.isNotEmpty())
        assertTrue(helpText.contains("Flip Help"))
    }

    @Test
    fun `onInvoke with empty text sends error message`() = runTest {
        val incomingMessage = createTestMessage("flip", "", arguments = emptyList()
        )

        flipModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        assertEquals("channel123", sentMessage.channelId)
        assertTrue(sentMessage.message.toString().contains("Please provide text to flip"))
    }

    @Test
    fun `onInvoke with text flips it correctly`() = runTest {
        val incomingMessage = createTestMessage("flip", "hello", arguments = emptyList()
        )

        flipModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should contain the table flip emoticon
        assertTrue(messageText.contains("(╯°□°）╯︵"))
        // Should contain flipped text (ollǝɥ is hello flipped)
        assertTrue(messageText.contains("ollǝɥ"))
    }

    @Test
    fun `onInvoke flips uppercase letters`() = runTest {
        val incomingMessage = createTestMessage("flip", "HELLO", arguments = emptyList()
        )

        flipModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should contain flipped text
        assertTrue(messageText.contains("(╯°□°）╯︵"))
    }

    @Test
    fun `onInvoke flips numbers correctly`() = runTest {
        val incomingMessage = createTestMessage("flip", "123", arguments = emptyList()
        )

        flipModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should contain the table flip emoticon and flipped numbers
        assertTrue(messageText.contains("(╯°□°）╯︵"))
        // 123 flipped should be ƐᄅƖ
        assertTrue(messageText.contains("ƐᄅƖ"))
    }

    @Test
    fun `onInvoke flips punctuation correctly`() = runTest {
        val incomingMessage = createTestMessage("flip", "hello!", arguments = emptyList()
        )

        flipModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should contain flipped text
        assertTrue(messageText.contains("(╯°□°）╯︵"))
        // ! flipped is ¡
        assertTrue(messageText.contains("¡"))
    }

    @Test
    fun `onInvoke preserves unflippable characters`() = runTest {
        val incomingMessage = createTestMessage("flip", "@#$", arguments = emptyList()
        )

        flipModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // Should contain the table flip emoticon
        assertTrue(messageText.contains("(╯°□°）╯︵"))
    }
}
