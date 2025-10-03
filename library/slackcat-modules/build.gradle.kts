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
        exposed()       // For storage modules (Kudos, Learn, Jeopardy)
        ktorClient()    // For network modules
    }
}

val slackcatProperties = SlackcatProperties(project)

dependencies {
    // Core framework
    api(projects.library.slackcat)

    // Required for storage modules
    api(projects.library.core.database)

    // Required for network modules
    api(projects.library.core.network)

    // Serialization for network modules
    implementation(libs.serialization.json)
}
