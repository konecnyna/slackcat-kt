package com.slackcat.modules.storage.kudos

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.MessageElement
import com.slackcat.common.SlackcatConfig
import com.slackcat.database.createTestDatabase
import com.slackcat.database.createTestSchema
import com.slackcat.database.dropTestSchema
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
    private lateinit var database: org.jetbrains.exposed.sql.Database

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)
        mockConfig = mockk(relaxed = true)

        every { mockConfig.botNameProvider() } returns "TestBot"
        every { mockConfig.botIconProvider() } returns mockk(relaxed = true)
        coEvery { mockChatClient.sendMessage(any(), any(), any()) } returns Result.success("mock_timestamp")
        coEvery { mockChatClient.updateMessage(any(), any(), any(), any(), any()) } returns
            Result.success("mock_timestamp")

        // Create a temporary SQLite database file for testing
        val dbFile = tempDir.resolve("test.db").toString()
        database = createTestDatabase("jdbc:sqlite:$dbFile", driver = "org.sqlite.JDBC")

        // Create the table schema synchronously
        createTestSchema(database, KudosDAO.KudosTable, KudosDAO.KudosMessageTable, KudosDAO.KudosTransactionTable)

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
        dropTestSchema(database, KudosDAO.KudosTransactionTable, KudosDAO.KudosMessageTable, KudosDAO.KudosTable)
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

            val hasUser456 =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("<@user456>")
                        is MessageElement.Heading -> element.content.contains("<@user456>")
                        else -> false
                    }
                }
            assertTrue(hasUser456)

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

            // First user gets sendMessage, second user gets updateMessage (same thread)
            coVerify(exactly = 1) { mockChatClient.sendMessage(any(), any(), any()) }
            coVerify(exactly = 1) { mockChatClient.updateMessage(any(), any(), any(), any(), any()) }
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

            val hasUser456 =
                sentMessage.content.elements.any { element ->
                    when (element) {
                        is MessageElement.Text -> element.content.contains("<@user456>")
                        is MessageElement.Heading -> element.content.contains("<@user456>")
                        else -> false
                    }
                }
            assertTrue(hasUser456)

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

            // First valid user gets sendMessage, second valid user gets updateMessage (same thread)
            // user123 is filtered out (self-kudos)
            coVerify(exactly = 1) { mockChatClient.sendMessage(any(), any(), any()) }
            coVerify(exactly = 1) { mockChatClient.updateMessage(any(), any(), any(), any(), any()) }
        }

    // Create a test subclass to access protected method
    private class TestKudosModule : KudosModule() {
        fun testGetKudosMessage(kudos: KudosDAO.KudosRow): String = getKudosMessage(kudos)
    }

    @Test
    fun `getKudosMessage returns correct message for 1 plus`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 1)
        val message = testModule.testGetKudosMessage(kudosRow)
        assertEquals("<@user123> now has 1 plus", message)
    }

    @Test
    fun `getKudosMessage returns correct message for 10 pluses`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 10)
        val message = testModule.testGetKudosMessage(kudosRow)
        assertEquals("<@user123> now has 10 pluses! Double digits!", message)
    }

    @Test
    fun `getKudosMessage returns correct message for 69 pluses`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 69)
        val message = testModule.testGetKudosMessage(kudosRow)
        assertEquals("Nice <@user123>", message)
    }

    @Test
    fun `getKudosMessage returns correct message for multiple pluses`() {
        val testModule = TestKudosModule()
        val kudosRow = KudosDAO.KudosRow(1, "user123", 5)
        val message = testModule.testGetKudosMessage(kudosRow)
        assertEquals("<@user123> now has 5 pluses", message)
    }
}
