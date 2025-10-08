package com.slackcat.internal

import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.CommandInfo
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
        mockModule =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(CommandInfo("testCommand"))
            }

        // Create a separate mock SlackcatModule with its own command and alias
        aliasModule =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(
                    CommandInfo(
                        command = "anotherCommand",
                        aliases = listOf("aliasForAnother"),
                    ),
                )
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
    fun `onMessage should handle aliases correctly`() =
        runTest {
            val chatUser = ChatUser("user123")
            val message =
                IncomingChatMessage(
                    arguments = emptyList(),
                    command = "?aliasForAnother",
                    channelId = "channel1",
                    chatUser = chatUser,
                    messageId = "msg123",
                    userText = "foo bar",
                    rawMessage = "?aliasForAnother Do something",
                )

            val result = router.onMessage(message)

            assertTrue(result)
            verify(aliasModule, times(1)).onInvoke(message)
            verify(mockModule, never()).onInvoke(message)
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

    @Test
    fun `Router should throw exception when duplicate commands are registered`() {
        val testScope = TestScope()
        val eventsFlow = MutableSharedFlow<SlackcatEvent>()

        val module1 =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(CommandInfo("duplicate"))
            }
        val module2 =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(CommandInfo("duplicate"))
            }

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            Router(listOf(module1, module2), testScope, eventsFlow)
        }
    }

    @Test
    fun `Router should throw exception when alias conflicts with command`() {
        val testScope = TestScope()
        val eventsFlow = MutableSharedFlow<SlackcatEvent>()

        val module1 =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(CommandInfo("mycommand"))
            }
        val module2 =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(
                    CommandInfo(
                        command = "other",
                        aliases = listOf("mycommand"),
                    ),
                )
            }

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            Router(listOf(module1, module2), testScope, eventsFlow)
        }
    }

    @Test
    fun `Router should throw exception when duplicate aliases are registered`() {
        val testScope = TestScope()
        val eventsFlow = MutableSharedFlow<SlackcatEvent>()

        val module1 =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(
                    CommandInfo(
                        command = "command1",
                        aliases = listOf("shared"),
                    ),
                )
            }
        val module2 =
            mock(SlackcatModule::class.java).apply {
                `when`(commandInfo()).thenReturn(
                    CommandInfo(
                        command = "command2",
                        aliases = listOf("shared"),
                    ),
                )
            }

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            Router(listOf(module1, module2), testScope, eventsFlow)
        }
    }
}
