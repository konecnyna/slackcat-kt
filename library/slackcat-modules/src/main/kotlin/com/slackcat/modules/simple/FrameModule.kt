package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule

open class FrameModule : SlackcatModule() {
    companion object {
        private const val MAX_EMOJIS = 100
        private val EMOJI_REGEX = Regex(":[a-zA-Z0-9_+\\-]+:")

        private const val TOP_LEFT = ":frame_top_left:"
        private const val TOP_CENTER = ":frame_top_center:"
        private const val TOP_RIGHT = ":frame_top_right:"
        private const val MIDDLE_LEFT = ":frame_middle_left:"
        private const val MIDDLE_RIGHT = ":frame_middle_right:"
        private const val BOTTOM_LEFT = ":frame_bottom_left:"
        private const val BOTTOM_CENTER = ":frame_bottom_center:"
        private const val BOTTOM_RIGHT = ":frame_bottom_right:"
    }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val input = incomingChatMessage.userText.trim()

        val grid =
            input.split("\n")
                .map { line -> EMOJI_REGEX.findAll(line.trim()).map { it.value }.toList() }
                .filter { it.isNotEmpty() }

        if (grid.isEmpty()) {
            sendHelpMessage(incomingChatMessage)
            return
        }

        val totalEmojis = grid.sumOf { it.size }
        if (totalEmojis > MAX_EMOJIS) {
            sendReply(incomingChatMessage, "Too many emojis! Max is $MAX_EMOJIS.")
            return
        }

        val message = buildFrameMessage(grid)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                threadId = null,
                content = message,
            ),
        )
    }

    private fun buildFrameMessage(grid: List<List<String>>): BotMessage {
        val width = grid.maxOf { it.size }
        val frame =
            buildString {
                append(TOP_LEFT)
                repeat(width) { append(TOP_CENTER) }
                append(TOP_RIGHT)
                append("\n")

                grid.forEach { row ->
                    append(MIDDLE_LEFT)
                    row.forEach { append(it) }
                    repeat(width - row.size) { append(":transparent:") }
                    append(MIDDLE_RIGHT)
                    append("\n")
                }

                append(BOTTOM_LEFT)
                repeat(width) { append(BOTTOM_CENTER) }
                append(BOTTOM_RIGHT)
            }

        return buildMessage {
            text(frame)
        }
    }

    private suspend fun sendHelpMessage(incomingChatMessage: IncomingChatMessage) {
        sendReply(
            incomingChatMessage,
            "Usage: `?frame :emoji1: :emoji2: ...`",
        )
    }

    private suspend fun sendReply(
        incomingChatMessage: IncomingChatMessage,
        text: String,
    ) {
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                threadId = incomingChatMessage.messageId,
                content = textMessage(text),
            ),
        )
    }

    override fun commandInfo() = CommandInfo(command = "frame", aliases = listOf("pf"))

    override fun help(): BotMessage =
        buildMessage {
            heading("Picture Frame")
            text("Wraps emojis in a decorative picture frame")
            text("\nUsage:")
            text("  `?frame :cat: :dog: :bird:` - Frame those emojis")
            text("  Multi-line input creates a grid frame")
            text("\n_Max $MAX_EMOJIS emojis per frame._")
        }
}
