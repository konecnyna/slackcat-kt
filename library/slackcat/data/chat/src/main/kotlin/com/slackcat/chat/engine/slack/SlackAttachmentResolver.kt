package com.slackcat.chat.engine.slack

import com.slack.api.model.block.RichTextBlock
import com.slack.api.model.block.element.RichTextSectionElement
import com.slack.api.model.block.element.RichTextUnknownElement
import com.slack.api.model.event.MessageEvent
import com.slackcat.chat.models.ChatAttachment

/**
 * Reads the files attached to a Slack message.
 *
 * Slack omits `files` from a message event when it renders a hosted file inline in a rich_text
 * block, and Bolt maps that element to [RichTextUnknownElement] without the file id. The API is
 * then the only source, so this falls back to it when the event shows a file it did not carry.
 */
open class SlackAttachmentResolver(private val apiOps: SlackApiOperations) {
    open suspend fun resolve(message: MessageEvent): List<ChatAttachment> {
        val eventFiles = message.files.orEmpty()
        if (eventFiles.isNotEmpty()) return eventFiles.mapNotNull { it.toChatAttachment() }
        if (!hasInlineFileElement(message)) return emptyList()
        return apiOps.getMessageFiles(message.channel, message.ts, message.threadTs)
            .getOrDefault(emptyList())
            .mapNotNull { it.toChatAttachment() }
    }

    /** Removes each attachment's bare file id, which Slack leaves in the message text. */
    fun stripAttachmentTokens(
        text: String,
        attachments: List<ChatAttachment>,
    ): String {
        if (attachments.isEmpty()) return text
        val stripped =
            attachments.fold(text) { acc, attachment ->
                acc.replace(Regex("""(?<!\S)${Regex.escape(attachment.id)}(?!\S)"""), "")
            }
        return stripped.replace(Regex("""[ \t]+"""), " ").trim()
    }

    fun hasInlineFileElement(message: MessageEvent): Boolean =
        message.blocks.orEmpty()
            .filterIsInstance<RichTextBlock>()
            .flatMap { it.elements.orEmpty() }
            .filterIsInstance<RichTextSectionElement>()
            .flatMap { it.elements.orEmpty() }
            .any { it is RichTextUnknownElement && it.type == "file" }

    private fun com.slack.api.model.File.toChatAttachment(): ChatAttachment? {
        val fileId = id ?: return null
        return ChatAttachment(
            id = fileId,
            name = name ?: "",
            mimetype = mimetype ?: "",
            urlPrivate = urlPrivate ?: "",
            permalink = permalink ?: "",
        )
    }
}
