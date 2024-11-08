package com.slackcat.plugins

import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension

val slackcatProperties = SlackcatProperties(project)
val slackcatExtension = extensions.create(
    "slackcat",
    SlackcatExtension::class.java,
    objects,
    slackcatProperties
)

afterEvaluate {
    slackcatExtension.applyTo(this)
}
