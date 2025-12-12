package com.slackcat.modules.storage.kudos

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
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Path

class KudosModuleTest {
    private lateinit var kudosModule: KudosModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope
    private lateinit var mockConfig: SlackcatConfig
    private lateinit var database: Database

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)
        mockConfig = mockk(relaxed = true)

        every { mockConfig.botNameProvider() } returns "TestBot"
        every { mockConfig.botIconProvider() } returns mockk(relaxed = true)
        coEvery { mockChatClient.sendMessage(any(), any(), any()) } returns Result.success(Unit)
        coEvery { mockChatClient.getUserDisplayName(any()) } returns Result.success("Test User")

        // Create a temporary SQLite database file for testing
        val dbFile = tempDir.resolve("test.db").toString()
        database = Database.connect("jdbc:sqlite:$dbFile", driver = "org.sqlite.JDBC")

        // Create the table schema synchronously
        transaction(database) {
            SchemaUtils.create(KudosDAO.KudosTable)
        }

        startKoin {
            modules(
                module {
                    single<ChatClient> { mockChatClient }
                    single<CoroutineScope> { mockCoroutineScope }
                    single<SlackcatConfig> { mockConfig }
                },
            )
        }

        kudosModule = KudosModule()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(KudosDAO.KudosTable)
        }
        stopKoin()
    }

    private fun createTestMessage(
        command: String,
        userText: String = "",
        channelId: String = "channel123",
        userId: String = "user123",
        messageId: String = "msg123",
        arguments: List<String> = emptyList(),
    ) = IncomingChatMessage(
        arguments = arguments,
        command = command,
        channelId = channelId,
        chatUser = ChatUser(userId),
        messageId = messageId,
        rawMessage = "?$command $userText",
        userText = userText,
    )

    @Test
    fun `provideCommand returns ++`() {
        assertEquals("++", kudosModule.commandInfo().command)
    }

    @Test
    fun `help returns non-empty string`() {
        val helpMessage = kudosModule.help()
        assertTrue(helpMessage.elements.isNotEmpty())
        // Check that help message contains heading or text with the expected content
        val hasExpectedContent =
            helpMessage.elements.any { element ->
                when (element) {
                    is MessageElement.Heading -> element.content.contains("KudosModule Help")
                    is MessageElement.Text -> element.content.contains("KudosModule Help")
                    else -> false
                }
            }
        assertTrue(hasExpectedContent)
    }

    @Test
    fun `onInvoke with single user mention gives kudos and responds in thread`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    command = "++",
                    userText = "<@user456>",
                    messageId = "msg123",
                )

            kudosModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            assertEquals("msg123", sentMessage.threadId)

            val hasTestUser =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Test User")
                        is MessageElement.Heading -> element.content.contains("Test User")
                        else -> false
                    }
                }
            assertTrue(hasTestUser)

            val hasOnePlus =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("1 plus")
                        is MessageElement.Heading -> element.content.contains("1 plus")
                        else -> false
                    }
                }
            assertTrue(hasOnePlus)
        }

    @Test
    fun `onInvoke with multiple user mentions gives kudos to each user`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    command = "++",
                    userText = "<@user456> <@user789>",
                    messageId = "msg123",
                )

            kudosModule.onInvoke(incomingMessage)

            val messageSlots = mutableListOf<OutgoingChatMessage>()
            coVerify(exactly = 2) { mockChatClient.sendMessage(capture(messageSlots), any(), any()) }

            messageSlots.forEach { sentMessage ->
                assertEquals("channel123", sentMessage.channelId)
                assertEquals("msg123", sentMessage.threadId)
            }
        }

    @Test
    fun `onInvoke with duplicate user mentions only gives kudos once`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    command = "++",
                    userText = "<@user456> <@user456> <@user456>",
                    messageId = "msg123",
                )

            kudosModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify(exactly = 1) { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured

            val hasTestUser =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("Test User")
                        is MessageElement.Heading -> element.content.contains("Test User")
                        else -> false
                    }
                }
            assertTrue(hasTestUser)

            val hasOnePlus =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("1 plus")
                        is MessageElement.Heading -> element.content.contains("1 plus")
                        else -> false
                    }
                }
            assertTrue(hasOnePlus)
        }

    @Test
    fun `onInvoke prevents users from plusing themselves and sends warning message`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    command = "++",
                    userText = "<@user123>",
                    userId = "user123",
                    messageId = "msg123",
                )

            kudosModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify(exactly = 1) { mockChatClient.sendMessage(capture(messageSlot), any(), any()) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            assertEquals("msg123", sentMessage.threadId)

            val hasWarningMessage =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("You'll go blind doing that!")
                        is MessageElement.Heading -> element.content.contains("You'll go blind doing that!")
                        else -> false
                    }
                }
            assertTrue(hasWarningMessage)
        }

    @Test
    fun `onInvoke prevents users from plusing themselves in mixed message`() =
        runTest {
            val incomingMessage =
                createTestMessage(
                    command = "++",
                    userText = "<@user123> <@user456> <@user789>",
                    userId = "user123",
                    messageId = "msg123",
                )

            kudosModule.onInvoke(incomingMessage)

            val messageSlots = mutableListOf<OutgoingChatMessage>()
            coVerify(exactly = 2) { mockChatClient.sendMessage(capture(messageSlots), any(), any()) }

            messageSlots.forEach { sentMessage ->
                assertEquals("channel123", sentMessage.channelId)
                assertEquals("msg123", sentMessage.threadId)

                val hasExpectedUser =
                    sentMessage.content.elements.any { element ->
                        when (element) {
                            is MessageElement.Text ->
                                element.content.contains("<@user456>") ||
                                    element.content.contains("<@user789>")
                            is MessageElement.Heading ->
                                element.content.contains("<@user456>") ||
                                    element.content.contains("<@user789>")
                            else -> false
                        }
                    }
                assertTrue(hasExpectedUser)
            }
        }

    // Create a test subclass to access protected method
    private class TestKudosModule : KudosModule() {
        fun testGetKudosMessage(
            kudos: KudosDAO.KudosRow,
            displayName: String,
        ): String = getKudosMessage(kudos, displayName)
    }

    @Test
    fun `getKudosMessage returns correct message for 1 plus`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 1)
        val message = testModule.testGetKudosMessage(kudosRow, "Test User")
        assertEquals("Test User now has 1 plus", message)
    }

    @Test
    fun `getKudosMessage returns correct message for 10 pluses`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 10)
        val message = testModule.testGetKudosMessage(kudosRow, "Test User")
        assertEquals("Test User now has 10 pluses! Double digits!", message)
    }

    @Test
    fun `getKudosMessage returns correct message for 69 pluses`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 69)
        val message = testModule.testGetKudosMessage(kudosRow, "Test User")
        assertEquals("Nice Test User", message)
    }

    @Test
    fun `getKudosMessage returns correct message for multiple pluses`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 5)
        val message = testModule.testGetKudosMessage(kudosRow, "Test User")
        assertEquals("Test User now has 5 pluses", message)
    }
}
