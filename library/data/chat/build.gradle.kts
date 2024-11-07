import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.slackcat.plugins.library")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

slackcat {
    features {
        coroutines()
        exposed()
        ktorServer()
    }
}

val slackcatProperties = SlackcatProperties(project)


repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.library.core.server)

    implementation(libs.slack.api.client)
    implementation(libs.slack.bolt)
    implementation(libs.slack.bolt.socket)
    implementation(libs.websocket.api)
    implementation(libs.tyrus.standalone.client)
    implementation(libs.slf4j.simple)
}