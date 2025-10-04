package com.slackcat.modules.storage.kudos

import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.database.DatabaseDriver
import io.mockk.coEvery
import io.mockk.coVerify
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

class KudosModuleTest {
    private lateinit var kudosModule: KudosModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        kudosModule = KudosModule()
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)

        DatabaseDriver.connect(inMemory = true)
        database = DatabaseDriver.getDatabase()

        transaction(database) {
            SchemaUtils.create(KudosDAO.KudosTable)
        }

        kudosModule.chatClient = mockChatClient
        kudosModule.coroutineScope = mockCoroutineScope

        coEvery { mockChatClient.sendMessage(any()) } returns Result.success(Unit)
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(KudosDAO.KudosTable)
        }
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
        assertEquals("++", kudosModule.provideCommand())
    }

    @Test
    fun `help returns non-empty string`() {
        val helpText = kudosModule.help()
        assertTrue(helpText.isNotEmpty())
        assertTrue(helpText.contains("KudosModule Help"))
    }

    @Test
    fun `onInvoke with single user mention gives kudos and responds in thread`() =
        runTest {
            val incomingMessage = createTestMessage(
                command = "++",
                userText = "<@user456>",
                messageId = "msg123",
            )

            kudosModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify { mockChatClient.sendMessage(capture(messageSlot)) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            assertEquals("msg123", sentMessage.threadId)
            assertTrue(sentMessage.message.toString().contains("<@user456>"))
            assertTrue(sentMessage.message.toString().contains("1 plus"))
        }

    @Test
    fun `onInvoke with multiple user mentions gives kudos to each user`() =
        runTest {
            val incomingMessage = createTestMessage(
                command = "++",
                userText = "<@user456> <@user789>",
                messageId = "msg123",
            )

            kudosModule.onInvoke(incomingMessage)

            val messageSlots = mutableListOf<OutgoingChatMessage>()
            coVerify(exactly = 2) { mockChatClient.sendMessage(capture(messageSlots)) }

            messageSlots.forEach { sentMessage ->
                assertEquals("channel123", sentMessage.channelId)
                assertEquals("msg123", sentMessage.threadId)
            }
        }

    @Test
    fun `onInvoke with duplicate user mentions only gives kudos once`() =
        runTest {
            val incomingMessage = createTestMessage(
                command = "++",
                userText = "<@user456> <@user456> <@user456>",
                messageId = "msg123",
            )

            kudosModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify(exactly = 1) { mockChatClient.sendMessage(capture(messageSlot)) }

            val sentMessage = messageSlot.captured
            assertTrue(sentMessage.message.toString().contains("<@user456>"))
            assertTrue(sentMessage.message.toString().contains("1 plus"))
        }

    @Test
    fun `onInvoke prevents users from plusing themselves and sends warning message`() =
        runTest {
            val incomingMessage = createTestMessage(
                command = "++",
                userText = "<@user123>",
                userId = "user123",
                messageId = "msg123",
            )

            kudosModule.onInvoke(incomingMessage)

            val messageSlot = slot<OutgoingChatMessage>()
            coVerify(exactly = 1) { mockChatClient.sendMessage(capture(messageSlot)) }

            val sentMessage = messageSlot.captured
            assertEquals("channel123", sentMessage.channelId)
            assertEquals("msg123", sentMessage.threadId)
            assertTrue(sentMessage.message.toString().contains("You'll go blind doing that!"))
        }

    @Test
    fun `onInvoke prevents users from plusing themselves in mixed message`() =
        runTest {
            val incomingMessage = createTestMessage(
                command = "++",
                userText = "<@user123> <@user456> <@user789>",
                userId = "user123",
                messageId = "msg123",
            )

            kudosModule.onInvoke(incomingMessage)

            val messageSlots = mutableListOf<OutgoingChatMessage>()
            coVerify(exactly = 2) { mockChatClient.sendMessage(capture(messageSlots)) }

            messageSlots.forEach { sentMessage ->
                assertEquals("channel123", sentMessage.channelId)
                assertEquals("msg123", sentMessage.threadId)
                val messageText = sentMessage.message.toString()
                assertTrue(messageText.contains("<@user456>") || messageText.contains("<@user789>"))
            }
        }

    @Test
    fun `getKudosMessage returns correct message for 1 plus`() {
        val kudosRow = KudosDAO.KudosRow(1, "user123", 1)
        val message = kudosModule.getKudosMessage(kudosRow)
        assertEquals("<@user123> now has 1 plus", message)
    }

    @Test
    fun `getKudosMessage returns correct message for 10 pluses`() {
        val kudosRow = KudosDAO.KudosRow(1, "user123", 10)
        val message = kudosModule.getKudosMessage(kudosRow)
        assertEquals("<@user123> now has 10 pluses! Double digits!", message)
    }

    @Test
    fun `getKudosMessage returns correct message for 69 pluses`() {
        val kudosRow = KudosDAO.KudosRow(1, "user123", 69)
        val message = kudosModule.getKudosMessage(kudosRow)
        assertEquals("Nice <@user123>", message)
    }

    @Test
    fun `getKudosMessage returns correct message for multiple pluses`() {
        val kudosRow = KudosDAO.KudosRow(1, "user123", 5)
        val message = kudosModule.getKudosMessage(kudosRow)
        assertEquals("<@user123> now has 5 pluses", message)
    }
}
