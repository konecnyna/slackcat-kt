package com.slackcat.plugins.extentsion

import com.slackcat.SlackcatProperties
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@SlackcatExtensionMarker
open class SlackcatExtension @Inject constructor(
    objects: ObjectFactory,
    properties: SlackcatProperties,
) {
    val featuresHandler = objects.newInstance<FeaturesHandler>(objects, properties)

    fun features(action: Action<FeaturesHandler>) {
        action.execute(featuresHandler)
    }

    fun applyTo(project: Project) {
        featuresHandler.applyTo(project)
    }
}