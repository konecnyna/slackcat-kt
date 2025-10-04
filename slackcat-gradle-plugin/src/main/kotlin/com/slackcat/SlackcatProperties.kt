package com.slackcat

import com.slackcat.utils.getOrCreateExtra
import org.gradle.api.Project

class SlackcatProperties private constructor(private val project: Project) {
    companion object {
        private const val CACHED_PROVIDER_EXT_NAME = "slackcat.properties.provider"

        operator fun invoke(project: Project): SlackcatProperties =
            project.getOrCreateExtra(
                CACHED_PROVIDER_EXT_NAME,
                ::SlackcatProperties,
            )
    }
}
