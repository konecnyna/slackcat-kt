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
}
