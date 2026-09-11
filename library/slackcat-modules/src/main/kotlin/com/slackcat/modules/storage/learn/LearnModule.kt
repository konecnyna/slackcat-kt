package com.slackcat.modules.storage.learn

import com.slackcat.chat.models.ChatAttachment
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.SlackLinkFormatter
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.database.DatabaseTable
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.models.UnhandledCommandModule

open class LearnModule(private var router: com.slackcat.internal.Router? = null) :
    SlackcatModule(),
    StorageModule,
    UnhandledCommandModule {
    private val learnFactory = LearnFactory()
    private val learnDAO = LearnDAO()
    private val aliasHandler = LearnAliasHandler(learnDAO)

    /**
     * Sets the router reference so the module can check for command conflicts.
     * This is called by the Router after initialization.
     */
    fun setRouter(router: com.slackcat.internal.Router) {
        this.router = router
    }

    override fun tables(): List<DatabaseTable> = LearnDAO.getDatabaseTables()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val aliasMessage = aliasHandler.handleAliases(incomingChatMessage)
        if (aliasMessage != null) {
            sendMessage(aliasMessage)
            return
        }

        val attachmentUrls = resolveAttachmentUrls(incomingChatMessage.attachments)
        if (attachmentUrls.isFailure) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content =
                        textMessage(
                            "Failed to read the attached file. ${attachmentUrls.exceptionOrNull()?.message}",
                        ),
                    threadId = incomingChatMessage.messageId,
                ),
            )
            return
        }

        val learnRequest =
            learnFactory.makeLearnRequest(
                withAttachmentUrls(incomingChatMessage, attachmentUrls.getOrThrow()),
            )
        if (learnRequest == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        // Validate that the learn key doesn't conflict with existing commands
        val conflictingModule =
            router?.let { r ->
                r.getAllModules().firstOrNull { module ->
                    val commandInfo = module.commandInfo()
                    commandInfo.command == learnRequest.learnKey ||
                        commandInfo.aliases.contains(learnRequest.learnKey)
                }
            }

        if (conflictingModule != null) {
            val message =
                "Cannot learn '${learnRequest.learnKey}' because it conflicts with an existing command " +
                    "from ${conflictingModule::class.java.simpleName}."
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content = textMessage(message),
                    threadId = incomingChatMessage.messageId,
                ),
            )
            return
        }

        val message =
            when (learnDAO.insertLearn(learnRequest)) {
                true ->
                    "I've learned ${learnRequest.learnKey} successfully. " +
                        "To recall use `?${learnRequest.learnKey}`"
                false ->
                    "Failed to learn ${learnRequest.learnKey}. " +
                        "Please make sure command syntax is: ?learn \"<key>\" \"<text>'\""
            }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage(message),
                threadId = incomingChatMessage.messageId,
            ),
        )
    }

    /**
     * Appends each attachment URL to the learned text so `?<key>` recalls the file.
     * Slack strips the file from `text`, so the URL is the only durable reference.
     */
    private fun withAttachmentUrls(
        message: IncomingChatMessage,
        urls: List<String>,
    ): IncomingChatMessage {
        if (urls.isEmpty()) return message
        val userText = (message.userText.trim() + "\n" + urls.joinToString("\n")).trim()
        return message.copy(userText = userText)
    }

    /**
     * An image must be publicly readable or the image block renders broken. A non-image keeps its
     * permalink, so the bot never shares an arbitrary document outside the workspace.
     */
    private suspend fun resolveAttachmentUrls(attachments: List<ChatAttachment>): Result<List<String>> {
        val urls = mutableListOf<String>()
        attachments.forEach { attachment ->
            if (attachment.isImage) {
                chatClient.getPublicAttachmentUrl(attachment.id).fold(
                    onSuccess = { urls.add(it) },
                    onFailure = { return Result.failure(it) },
                )
            } else {
                urls.add(attachment.permalink)
            }
        }
        return Result.success(urls)
    }

    override suspend fun onUnhandledCommand(message: IncomingChatMessage): Boolean {
        val index =
            try {
                message.userText.toInt() - 1
            } catch (exception: NumberFormatException) {
                null
            }

        learnDAO.getLearn(key = message.command, index = index).fold(
            onSuccess = {
                sendLearnMessage(channelId = message.channelId, learnItem = it)
                return true
            },
            onFailure = {
                return false
            },
        )
    }

    private suspend fun sendLearnMessage(
        channelId: String,
        learnItem: LearnDAO.LearnRow,
    ) {
        val imageUrl = renderableImageUrl(learnItem.learnText)
        when (imageUrl != null) {
            true -> {
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        content =
                            buildMessage {
                                image(
                                    url = imageUrl,
                                    altText = "learn image",
                                )
                            },
                    ),
                )
            }

            false -> {
                sendMessage(
                    OutgoingChatMessage(
                        channelId = channelId,
                        content = textMessage(learnItem.learnText),
                    ),
                )
            }
        }
    }

    /**
     * Returns the URL to render as an image block, or null to post the entry as text.
     * The entry must be a lone image link. Slack-hosted private files need a bearer token,
     * so an image block pointing at one renders broken.
     */
    private fun renderableImageUrl(learnText: String): String? {
        val url = SlackLinkFormatter.extractFirstUrl(learnText) ?: return null
        if (SlackLinkFormatter.toBareUrls(learnText).trim() != url) return null
        if (!SlackLinkFormatter.isImageUrl(url)) return null
        if (SlackLinkFormatter.isPrivateSlackFileUrl(url)) return null
        return url
    }

    override fun help(): BotMessage =
        buildMessage {
            heading("LearnModule Help")
            text("Create a custom command to recall text.")
            text("*Usage:* ?learn \"<key>\" \"<text>'\"")
            text("You can then recall the text ?<key>")
        }

    override fun commandInfo() =
        CommandInfo(
            command = "learn",
            aliases = LearnAliases.entries.map { it.alias },
        )
}

enum class LearnAliases(val alias: String) {
    Unlearn("unlearn"),
    List("list"),
    ;

    companion object {
        private val aliasMap = entries.associateBy { it.alias }

        fun fromAlias(alias: String): LearnAliases? {
            return aliasMap[alias.lowercase()]
        }
    }
}
