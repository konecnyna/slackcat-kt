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
        ktorServer()
    }
}

val slackcatProperties = SlackcatProperties(project)

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.library.slackcat.core.server)
    implementation(projects.library.slackcat.core.common)

    implementation(libs.bundles.slack)
    implementation(libs.bundles.testing)
}
