package com.slackcat.modules.simple

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatConfig
import com.slackcat.internal.Router
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ModulesModuleTest {
    private lateinit var modulesModule: ModulesModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope
    private lateinit var mockRouter: Router
    private lateinit var mockConfig: SlackcatConfig

    @BeforeEach
    fun setup() {
        mockRouter = mockk(relaxed = true)
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

        modulesModule = ModulesModule(mockRouter)
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
    fun `provideCommand returns modules`() {
        assertEquals("modules", modulesModule.provideCommand())
    }

    @Test
    fun `aliases returns commands and list`() {
        val aliases = modulesModule.aliases()
        assertEquals(2, aliases.size)
        assertTrue(aliases.contains("commands"))
        assertTrue(aliases.contains("list"))
    }

    @Test
    fun `help returns non-empty string`() {
        val helpText = modulesModule.help()
        assertTrue(helpText.isNotEmpty())
        assertTrue(helpText.contains("ModulesModule Help"))
        assertTrue(helpText.contains("modules"))
    }

    @Test
    fun `onInvoke sends modules list`() =
        runTest {
            // Setup some test modules
            val pingModule = PingModule()
            val dateModule = DateModule()
            every { mockRouter.getAllModules() } returns listOf(pingModule, dateModule, modulesModule)

            val incomingMessage =
                createTestMessage(
                    "modules",
                    "",
                    arguments = emptyList(),
                )

            modulesModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)

            val messageText = sentMessage.message.toString()
            assertTrue(messageText.contains("Active Slackcat Modules"))
            assertTrue(messageText.contains("ping"))
            assertTrue(messageText.contains("date"))
            // ModulesModule should be filtered out
            assertFalse(messageText.contains("?modules"))
        }

    @Test
    fun `buildModulesList groups modules by category`() =
        runTest {
            val pingModule = PingModule()
            val dateModule = DateModule()
            every { mockRouter.getAllModules() } returns listOf(pingModule, dateModule)

            val incomingMessage = createTestMessage("modules")
            modulesModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val messageText = messageSlot.captured.message.toString()
            assertTrue(messageText.contains("Simple"))
        }

    @Test
    fun `buildModulesList includes module count`() =
        runTest {
            val pingModule = PingModule()
            val dateModule = DateModule()
            val flipModule = FlipModule()
            every { mockRouter.getAllModules() } returns listOf(pingModule, dateModule, flipModule)

            val incomingMessage = createTestMessage("modules")
            modulesModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val messageText = messageSlot.captured.message.toString()
            assertTrue(messageText.contains("Total: 3 modules"))
        }

    @Test
    fun `buildModulesList shows help usage hint`() =
        runTest {
            every { mockRouter.getAllModules() } returns listOf(PingModule())

            val incomingMessage = createTestMessage("modules")
            modulesModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val messageText = messageSlot.captured.message.toString()
            assertTrue(messageText.contains("--help"))
        }
}
