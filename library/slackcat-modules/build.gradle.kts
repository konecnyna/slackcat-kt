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
    }
}

val slackcatProperties = SlackcatProperties(project)

dependencies {
    // Core framework provides all necessary dependencies via api
    implementation(projects.library.slackcat)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
