package com.slackcat.app.modules.learn

import com.slackcat.chat.models.IncomingChatMessage

class LearnFactory {
    fun makeLearnRequest(incomingChatMessage: IncomingChatMessage): LearnInsertRow? {
        if (incomingChatMessage.userText.isEmpty()) return null

        val regex = """^\?learn\s+"(\w+)"\s+"((?s).+)"$""".toRegex()
        val match = regex.matchEntire(incomingChatMessage.userText.trim())

        return match?.let {
            val learnKey = it.groups[1]?.value
            val learnText = it.groups[2]?.value
            if (learnKey != null && learnText != null) {
                LearnInsertRow(
                    learnedBy = incomingChatMessage.chatUser.userId,
                    learnKey = learnKey,
                    learnText = learnText,
                )
            } else {
                null
            }
        }
    }
}