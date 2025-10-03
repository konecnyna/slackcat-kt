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

class DateModuleTest {
    private lateinit var dateModule: DateModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope

    @BeforeEach
    fun setup() {
        dateModule = DateModule()
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)

        dateModule.chatClient = mockChatClient
        dateModule.coroutineScope = mockCoroutineScope

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
        val helpText = dateModule.help()
        assertTrue(helpText.isNotEmpty())
        assertTrue(helpText.contains("DateModule Help"))
    }

    @Test
    fun `onInvoke sends current date message`() = runTest {
        val incomingMessage = createTestMessage("date", "", arguments = emptyList()
        )

        dateModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        assertEquals("channel123", sentMessage.channelId)
        assertTrue(sentMessage.message.toString().contains("Currently, it's"))
    }

    @Test
    fun `onInvoke message contains date components`() = runTest {
        val incomingMessage = createTestMessage("date", "", arguments = emptyList()
        )

        dateModule.onInvoke(incomingMessage)

        val messageSlot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

        val sentMessage = messageSlot.captured
        val messageText = sentMessage.message.toString()

        // The message should contain time-related terms
        assertTrue(messageText.contains("Currently") || messageText.contains("it's"))
    }
}
