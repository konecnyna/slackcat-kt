import com.slackcat.SlackcatProperties

plugins {
    id("com.slackcat.plugins.internal-library")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

slackcat {
    features {
        coroutines()
        ktorServer()
    }
}

val slackcatProperties = SlackcatProperties(project)

repositories {
    mavenCentral()
}

dependencies {
}
