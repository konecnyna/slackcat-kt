package com.slackcat.presentation

class MessageBuilder {
    private val content = StringBuilder()

    // Slack does not have titles, but you could simulate one with bold text and a newline
    fun title(title: String) {
        content.append("*$title*\n")
    }

    fun text(text: String) {
        content.append("$text\n")
    }

    fun bold(text: String) {
        content.append("*$text*\n")
    }

    fun url(
        url: String,
        text: String,
    ) {
        content.append("<$url|$text>\n")
    }

    override fun toString(): String = content.toString()
}

inline fun buildMessage(builderAction: MessageBuilder.() -> Unit): String {
    val builder = MessageBuilder()
    builder.builderAction()
    return builder.toString()
}
