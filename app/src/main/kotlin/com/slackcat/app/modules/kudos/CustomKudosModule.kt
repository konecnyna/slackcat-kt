package com.slackcat.app.modules.kudos

import com.slackcat.modules.storage.kudos.KudosModule

class CustomKudosModule : KudosModule() {
    override val spamProtectionEnabled = false
}
