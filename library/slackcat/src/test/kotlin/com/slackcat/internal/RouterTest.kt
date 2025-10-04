package com.slackcat.internal

import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class RouterTest {
    private lateinit var router: Router
    private lateinit var mockModule: SlackcatModule
    private lateinit var aliasModule: SlackcatModule

    @BeforeEach
    fun setup() {
        // Create a mock SlackcatModule with a command "testCommand"
        // Create a mock SlackcatModule with a command "testCommand"
        mockModule =
            mock(SlackcatModule::class.java).apply {
                `when`(provideCommand()).thenReturn("testCommand")
            }

        // Create a separate mock SlackcatModule for an alias
        aliasModule =
            mock(SlackcatModule::class.java).apply {
                `when`(provideCommand()).thenReturn("anotherCommand")
                `when`(aliases()).thenReturn(listOf("testCommand")) // This module is an alias
            }

        // Pass both modules to the Router
        val testScope = TestScope()
        val eventsFlow = MutableSharedFlow<SlackcatEvent>()
        router = Router(listOf(mockModule, aliasModule), testScope, eventsFlow)
    }

    @Test
    fun `onMessage should return true when a valid command is processed`() =
        runTest {
            val chatUser = ChatUser("user123")
            val message =
                IncomingChatMessage(
                    arguments = emptyList(),
                    command = "?testCommand",
                    channelId = "channel1",
                    chatUser = chatUser,
                    messageId = "msg123",
                    userText = "foo bar",
                    rawMessage = "?testCommand Do something",
                )

            val result = router.onMessage(message)

            // Verify that the command was handled
            assertTrue(result)
            verify(mockModule, times(1)).onInvoke(message)
        }

    @Test
    fun `onMessage should return false when message does not start with question mark`() =
        runTest {
            val chatUser = ChatUser("user123")
            val message =
                IncomingChatMessage(
                    arguments = emptyList(),
                    command = "testCommand",
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
    fun `onMessage should return false when command does not exist in featureCommandMap`() =
        runTest {
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
    fun `onMessage should return false when command extraction fails`() =
        runTest {
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
    fun `onMessage should return false when message is empty`() =
        runTest {
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

    @Test
    fun `onMessage should prioritize featureCommandMap over aliasCommandMap`() =
        runTest {
            val chatUser = ChatUser("user123")
            val message =
                IncomingChatMessage(
                    arguments = emptyList(),
                    command = "?testCommand",
                    channelId = "channel1",
                    chatUser = chatUser,
                    messageId = "msg123",
                    userText = "foo bar",
                    rawMessage = "?testCommand Do something",
                )

            val result = router.onMessage(message)

            assertTrue(result)
            verify(mockModule, times(1)).onInvoke(message)
            verify(aliasModule, never()).onInvoke(message)
        }

    @Test
    fun `onMessage should use aliasCommandMap when featureCommandMap does not have the command`() =
        runTest {
            val chatUser = ChatUser("user123")
            val message =
                IncomingChatMessage(
                    arguments = emptyList(),
                    command = "?anotherCommand",
                    channelId = "channel1",
                    chatUser = chatUser,
                    messageId = "msg123",
                    userText = "foo bar",
                    rawMessage = "?anotherCommand Do something",
                )

            // When `anotherCommand` is called, it should resolve to `aliasModule` (from aliasCommandMap)
            val result = router.onMessage(message)

            assertTrue(result)
            verify(aliasModule, times(1)).onInvoke(message)
            verify(mockModule, never()).onInvoke(message)
        }

    @Test
    fun `onMessage should return false when command is not in featureCommandMap or aliasCommandMap`() =
        runTest {
            val chatUser = ChatUser("user123")
            val message =
                IncomingChatMessage(
                    arguments = emptyList(),
                    command = "?unknownCommand",
                    channelId = "channel1",
                    chatUser = chatUser,
                    messageId = "msg123",
                    userText = "foo bar",
                    rawMessage = "?unknownCommand Do something",
                )

            val result = router.onMessage(message)

            assertFalse(result)
            verify(mockModule, never()).onInvoke(message)
            verify(aliasModule, never()).onInvoke(message)
        }
}
