package com.slackcat.common

data class RichTextMessage(
    val text: String,
    val attachments: List<MessageAttachment> = emptyList(),
)

data class MessageAttachment(
    val color: String,
    val blocks: String,
)
