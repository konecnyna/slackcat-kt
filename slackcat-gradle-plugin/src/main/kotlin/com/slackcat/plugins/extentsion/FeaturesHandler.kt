package com.slackcat.plugins.extentsion

import com.slackcat.SlackcatProperties
import com.slackcat.utils.setDisallowChanges
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class FeaturesHandler
    @Inject
    constructor(
        objects: ObjectFactory,
        val properties: SlackcatProperties,
    ) {
        private val coroutines: Property<Boolean> = objects.property<Boolean>().convention(false)
        private val exposed: Property<Boolean> = objects.property<Boolean>().convention(false)
        private val ktorClient: Property<Boolean> = objects.property<Boolean>().convention(false)
        private val ktorServer: Property<Boolean> = objects.property<Boolean>().convention(false)
        private val reflection: Property<Boolean> = objects.property<Boolean>().convention(false)

        fun coroutines() = coroutines.setDisallowChanges(true)

        fun exposed() = exposed.setDisallowChanges(true)

        fun ktorClient() = ktorClient.setDisallowChanges(true)

        fun ktorServer() = ktorServer.setDisallowChanges(true)

        fun reflection() = reflection.setDisallowChanges(true)

        internal fun applyTo(project: Project) =
            with(project) {
                if (coroutines.get()) {
                    dependencies.add("compileOnly", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
                    dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                }

                if (exposed.get()) {
                    dependencies.add("implementation", "org.jetbrains.exposed:exposed-core:0.44.1")
                    dependencies.add("runtimeOnly", "org.jetbrains.exposed:exposed-dao:0.44.1")
                    dependencies.add("runtimeOnly", "org.jetbrains.exposed:exposed-jdbc:0.44.1")
                    dependencies.add("runtimeOnly", "org.xerial:sqlite-jdbc:3.34.0")
                }

                if (ktorClient.get()) {
                    dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
                    dependencies.add("implementation", "io.ktor:ktor-client-core:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-client-cio:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-client-serialization:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-client-content-negotiation:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-client-logging:2.3.6")
                }

                if (ktorServer.get()) {
                    dependencies.add("implementation", "io.ktor:ktor-server-netty:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-server-core:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-server-content-negotiation:2.3.6")
                    dependencies.add("implementation", "io.ktor:ktor-serialization-kotlinx-json:2.3.6")
                    dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
                    dependencies.add("implementation", "ch.qos.logback:logback-classic:1.4.14")
                }

                if (reflection.get()) {
                    dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
                }
            }
    }
