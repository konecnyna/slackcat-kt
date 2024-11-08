package com.slackcat.internal

import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.models.SlackcatModule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class RouterTest {
    private lateinit var router: Router
    private lateinit var mockModule: SlackcatModule

    @BeforeEach
    fun setup() {
        // Create a mock SlackcatModule with a command "testCommand"
        mockModule =
            mock(SlackcatModule::class.java).apply {
                `when`(provideCommand()).thenReturn("testCommand")
            }
        router = Router(listOf(mockModule))
    }

    @Test
    fun `onMessage should return true when a valid command is processed`() {
        val chatUser = ChatUser("user123")
        val message =
            IncomingChatMessage(
                arguments = emptyList(),
                command = "?ping",
                channelId = "channel1",
                chatUser = chatUser,
                messageId = "msg123",
                userText = "foo bar",
                rawMessage = "?testCommand Do something",
            )

        // Mock onInvoke to do nothing
        doNothing().`when`(mockModule).onInvoke(message)

        val result = router.onMessage(message)

        // Verify that the command was handled
        assertTrue(result)
        verify(mockModule, times(1)).onInvoke(message)
    }

    @Test
    fun `onMessage should return false when message does not start with question mark`() {
        val chatUser = ChatUser("user123")
        val message =
            IncomingChatMessage(
                arguments = emptyList(),
                command = "?ping",
                channelId = "channel1",
                chatUser = chatUser,
                messageId = "msg123",
                userText = "foo bar",
                rawMessage = "testCommand Do something",
            )

        val result = router.onMessage(message)

        // Verify that the command was not handled
        assertFalse(result)
        verify(mockModule, never()).onInvoke(message)
    }

    @Test
    fun `onMessage should return false when command does not exist in featureCommandMap`() {
        val chatUser = ChatUser("user123")
        val message =
            IncomingChatMessage(
                arguments = emptyList(),
                command = "?ping",
                channelId = "channel1",
                chatUser = chatUser,
                messageId = "msg123",
                userText = "foo bar",
                rawMessage = "?unknownCommand Do something",
            )

        val result = router.onMessage(message)

        // Verify that the command was not handled
        assertFalse(result)
        verify(mockModule, never()).onInvoke(message)
    }

    @Test
    fun `onMessage should return false when command extraction fails`() {
        val chatUser = ChatUser("user123")
        val message =
            IncomingChatMessage(
                arguments = emptyList(),
                command = "?ping",
                userText = "foo bar",
                channelId = "channel1",
                chatUser = chatUser,
                messageId = "msg123",
                rawMessage = "? Do something",
            )

        val result = router.onMessage(message)

        // Verify that the command was not handled
        assertFalse(result)
        verify(mockModule, never()).onInvoke(message)
    }

    @Test
    fun `onMessage should return false when message is empty`() {
        val chatUser = ChatUser("user123")
        val message =
            IncomingChatMessage(
                arguments = emptyList(),
                command = "?ping",
                channelId = "channel1",
                chatUser = chatUser,
                messageId = "msg123",
                userText = "foo bar",
                rawMessage = "",
            )

        val result = router.onMessage(message)

        // Verify that the command was not handled
        assertFalse(result)
        verify(mockModule, never()).onInvoke(message)
    }
}
