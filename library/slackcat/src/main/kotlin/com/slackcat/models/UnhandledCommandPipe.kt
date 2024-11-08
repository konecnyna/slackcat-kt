package com.slackcat.models

import com.slackcat.chat.models.IncomingChatMessage

interface UnhandledCommandPipe {
    /**
     * Return true handled command
     * Return false to delegate command
     */
    fun onUnhandledCommand(message: IncomingChatMessage)
}
