package com.slackcat.plugins.extentsion


import com.slackcat.SlackcatProperties
import com.slackcat.utils.setDisallowChanges
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject


abstract class FeaturesHandler @Inject constructor(
    objects: ObjectFactory,
    val properties: SlackcatProperties,
) {
    private val ktor: Property<Boolean> = objects.property<Boolean>().convention(false)
    private val coroutines: Property<Boolean> = objects.property<Boolean>().convention(false)
    private val reflection: Property<Boolean> = objects.property<Boolean>().convention(false)


    fun reflection() {
        reflection.setDisallowChanges(true)
    }

    fun ktor() {
        ktor.setDisallowChanges(true)
    }

    fun coroutines() {
        coroutines.setDisallowChanges(true)
    }


    internal fun applyTo(project: Project) = with(project) {
        if (ktor.get()) {
            dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
            dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
            dependencies.add("implementation", "io.ktor:ktor-client-core:1.6.7")
            dependencies.add("implementation", "io.ktor:ktor-client-cio:1.6.7")
            dependencies.add("implementation", "io.ktor:ktor-client-serialization:1.6.7")
        }

        if (coroutines.get()) {
            dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
        }

        if (reflection.get()) {
            dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        }
    }
}
