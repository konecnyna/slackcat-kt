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
        exposed()
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
    implementation("com.slack.api:bolt:1.44.1")
    implementation("com.slack.api:bolt-socket-mode:1.44.1")
    implementation("javax.websocket:javax.websocket-api:1.1")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.20")
    implementation("org.slf4j:slf4j-simple:1.7.36")
}