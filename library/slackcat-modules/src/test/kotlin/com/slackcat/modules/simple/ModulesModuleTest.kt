package com.slackcat.modules.simple

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.MessageElement
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
        assertEquals("modules", modulesModule.commandInfo().command)
    }

    @Test
    fun `aliases returns commands`() {
        val aliases = modulesModule.commandInfo().aliases
        assertEquals(1, aliases.size)
        assertTrue(aliases.contains("commands"))
    }

    @Test
    fun `help returns non-empty string`() {
        val helpMessage = modulesModule.help()
        assertTrue(helpMessage.elements.isNotEmpty())
        // Check that help message contains heading or text with the expected content
        val hasExpectedContent =
            helpMessage.elements.any { element ->
                when (element) {
                    is MessageElement.Heading ->
                        element.content.contains("ModulesModule Help") ||
                            element.content.contains("modules")
                    is MessageElement.Text ->
                        element.content.contains("ModulesModule Help") ||
                            element.content.contains("modules")
                    else -> false
                }
            }
        assertTrue(hasExpectedContent)
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

            val hasActiveModules =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Active Slackcat Modules")
                        is MessageElement.Heading -> element.content.contains("Active Slackcat Modules")
                        else -> false
                    }
                }
            assertTrue(hasActiveModules)

            val hasPing =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("ping")
                        is MessageElement.Heading -> element.content.contains("ping")
                        else -> false
                    }
                }
            assertTrue(hasPing)

            val hasDate =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("date")
                        is MessageElement.Heading -> element.content.contains("date")
                        else -> false
                    }
                }
            assertTrue(hasDate)

            // ModulesModule should be filtered out
            val hasModulesCommand =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("?modules")
                        is MessageElement.Heading -> element.content.contains("?modules")
                        else -> false
                    }
                }
            assertFalse(hasModulesCommand)
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

            val sentMessage = messageSlot.captured
            val hasSimple =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Simple")
                        is MessageElement.Heading -> element.content.contains("Simple")
                        else -> false
                    }
                }
            assertTrue(hasSimple)
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

            val sentMessage = messageSlot.captured
            val hasModuleCount =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Total: 3 modules")
                        is MessageElement.Heading -> element.content.contains("Total: 3 modules")
                        else -> false
                    }
                }
            assertTrue(hasModuleCount)
        }

    @Test
    fun `buildModulesList shows help usage hint`() =
        runTest {
            every { mockRouter.getAllModules() } returns listOf(PingModule())

            val incomingMessage = createTestMessage("modules")
            modulesModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            val hasHelpHint =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("--help")
                        is MessageElement.Heading -> element.content.contains("--help")
                        else -> false
                    }
                }
            assertTrue(hasHelpHint)
        }
}
