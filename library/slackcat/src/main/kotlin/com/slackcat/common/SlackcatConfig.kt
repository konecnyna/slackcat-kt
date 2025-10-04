package com.slackcat.common

import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.BotIcon.BotImageIcon
import com.slackcat.common.SlackcatAppDefaults.DEFAULT_BOT_IMAGE_ICON
import com.slackcat.common.SlackcatAppDefaults.DEFAULT_BOT_NAME

data class SlackcatConfig(
    val botNameProvider: () -> String = { DEFAULT_BOT_NAME },
    val botIconProvider: () -> BotIcon = { BotImageIcon(DEFAULT_BOT_IMAGE_ICON) },
)
