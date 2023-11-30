import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.slackcat.plugins.application")
    kotlin("plugin.serialization") version "1.5.21"
}

repositories {
    mavenCentral()
}


slackcat {
    features {
        coroutines()
        ktor()
        reflection()
    }
}

val slackcatProperties = SlackcatProperties(project)


repositories {
    mavenCentral()
}

dependencies {
    // Slack stuff
    implementation("com.slack.api:slack-api-client:1.8.1")
}