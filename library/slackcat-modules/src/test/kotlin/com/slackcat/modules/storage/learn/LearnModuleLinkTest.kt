package com.slackcat.modules.storage.learn

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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Path

class LearnModuleLinkTest {
    private lateinit var learnModule: LearnModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var database: Database

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        mockChatClient = mockk(relaxed = true)
        val mockConfig = mockk<SlackcatConfig>(relaxed = true)
        every { mockConfig.botNameProvider() } returns "TestBot"
        every { mockConfig.botIconProvider() } returns mockk(relaxed = true)
        coEvery { mockChatClient.sendMessage(any(), any(), any()) } returns Result.success("ts")

        database = Database.connect("jdbc:sqlite:${tempDir.resolve("learn.db")}", driver = "org.sqlite.JDBC")
        transaction(database) { SchemaUtils.create(LearnDAO.LearnTable) }

        startKoin {
            modules(
                module {
                    single<ChatClient> { mockChatClient }
                    single<CoroutineScope> { mockk<CoroutineScope>(relaxed = true) }
                    single<SlackcatConfig> { mockConfig }
                },
            )
        }

        learnModule = LearnModule()
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun seed(
        key: String,
        text: String,
    ) = transaction(database) {
        LearnDAO.LearnTable.insert {
            it[learnedBy] = "user123"
            it[learnKey] = key
            it[learnText] = text
        }
    }

    private suspend fun recall(key: String): OutgoingChatMessage {
        learnModule.onUnhandledCommand(
            IncomingChatMessage(
                arguments = emptyList(),
                command = key,
                channelId = "channel123",
                chatUser = ChatUser("user123"),
                messageId = "msg123",
                rawMessage = "?$key",
                userText = "",
            ),
        )
        val slot = slot<OutgoingChatMessage>()
        coVerify { mockChatClient.sendMessage(capture(slot), any(), any()) }
        return slot.captured
    }

    private fun OutgoingChatMessage.imageUrl(): String? =
        content.elements.filterIsInstance<MessageElement.Image>().firstOrNull()?.url

    private fun OutgoingChatMessage.textContent(): String =
        content.elements.filterIsInstance<MessageElement.Text>().joinToString("\n") { it.content }

    @Test
    fun `recall renders a Slack labeled image link as an image`() =
        runTest {
            seed("cat", "<https://example.com/cat.png|https://example.com/cat.png>")

            assertEquals("https://example.com/cat.png", recall("cat").imageUrl())
        }

    @Test
    fun `recall renders a standard markdown image link as an image`() =
        runTest {
            seed("dog", "[dog](https://example.com/dog.png)")

            assertEquals("https://example.com/dog.png", recall("dog").imageUrl())
        }

    @Test
    fun `recall preserves user mentions instead of unwrapping them`() =
        runTest {
            seed("greet", "hello <@U012AB3CD> welcome")

            assertEquals("hello <@U012AB3CD> welcome", recall("greet").textContent())
        }

    @Test
    fun `recall preserves channel links and special mentions`() =
        runTest {
            seed("ping", "see <#C123ABC456|general> and <!here>")

            assertEquals("see <#C123ABC456|general> and <!here>", recall("ping").textContent())
        }

    @Test
    fun `recall keeps prose markdown intact for the platform converter to format`() =
        runTest {
            seed("docs", "read the [docs](https://docs.slack.dev) today")

            assertEquals("read the [docs](https://docs.slack.dev) today", recall("docs").textContent())
        }

    @Test
    fun `recall posts a private Slack file image as a link not an image block`() =
        runTest {
            seed("file", "<https://files.slack.com/files-pri/T1-F1/cat.png|cat.png>")

            val message = recall("file")
            assertEquals(null, message.imageUrl())
            assertEquals("<https://files.slack.com/files-pri/T1-F1/cat.png|cat.png>", message.textContent())
        }
}
