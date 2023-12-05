import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.slackcat.plugins.library")
    kotlin("plugin.serialization") version "1.5.21"
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
    // Slack stuff
    implementation("com.slack.api:slack-api-client:1.8.1")
    implementation(project(":core:server"))
}