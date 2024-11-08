import com.slackcat.SlackcatProperties

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
        ktorClient()
    }
}

val slackcatProperties = SlackcatProperties(project)


repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.library.data.chat)
    implementation(projects.library.core.database)
    implementation(projects.library.core.common)
    implementation(projects.library.core.network)

    testImplementation(libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()
}