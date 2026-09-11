package com.slackcat.modules.storage.learn

import com.slackcat.chat.models.ChatAttachment
import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.ChatUser
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.common.SlackcatConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
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

class LearnModuleAttachmentTest {
    private lateinit var learnModule: LearnModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var database: Database

    @TempDir
    lateinit var tempDir: Path

    private val publicImageUrl = "https://files.slack.com/files-pri/T1-F1/image.png?pub_secret=abc123"

    private val imageAttachment =
        ChatAttachment(
            id = "F0C13M1K7DH",
            name = "image.png",
            mimetype = "image/png",
            urlPrivate = "https://files.slack.com/files-pri/T1-F1/image.png",
            permalink = "https://team.slack.com/files/U1/F0C13M1K7DH/image.png",
        )

    @BeforeEach
    fun setup() {
        mockChatClient = mockk(relaxed = true)
        val mockConfig = mockk<SlackcatConfig>(relaxed = true)
        every { mockConfig.botNameProvider() } returns "TestBot"
        every { mockConfig.botIconProvider() } returns mockk(relaxed = true)
        coEvery { mockChatClient.sendMessage(any(), any(), any()) } returns Result.success("ts")
        coEvery { mockChatClient.getPublicAttachmentUrl("F0C13M1K7DH") } returns Result.success(publicImageUrl)

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

    private suspend fun learn(
        userText: String,
        attachments: List<ChatAttachment>,
    ) = learnModule.onInvoke(
        IncomingChatMessage(
            arguments = emptyList(),
            command = "learn",
            channelId = "channel123",
            chatUser = ChatUser("user123"),
            messageId = "msg123",
            rawMessage = "?learn $userText",
            userText = userText,
            attachments = attachments,
        ),
    )

    private fun storedText(key: String): String? =
        transaction(database) {
            LearnDAO.LearnTable
                .selectAll()
                .firstOrNull { it[LearnDAO.LearnTable.learnKey] == key }
                ?.get(LearnDAO.LearnTable.learnText)
        }

    @Test
    fun `learn saves the public image url when the message carries only an image`() =
        runTest {
            learn("imgonnasayit", listOf(imageAttachment))

            assertEquals(publicImageUrl, storedText("imgonnasayit"))
        }

    @Test
    fun `learn appends the public image url after the text`() =
        runTest {
            learn("imgonnasayit dang", listOf(imageAttachment))

            assertEquals("dang\n$publicImageUrl", storedText("imgonnasayit"))
        }

    @Test
    fun `learn saves the permalink for a non image attachment`() =
        runTest {
            val document = imageAttachment.copy(mimetype = "application/pdf", name = "spec.pdf")

            learn("spec", listOf(document))

            assertEquals(document.permalink, storedText("spec"))
        }

    @Test
    fun `learn stores nothing when the public url lookup fails`() =
        runTest {
            coEvery { mockChatClient.getPublicAttachmentUrl("F0C13M1K7DH") } returns
                Result.failure(Exception("Slack API error: not_allowed_token_type"))

            learn("imgonnasayit", listOf(imageAttachment))

            assertEquals(null, storedText("imgonnasayit"))
        }
}
