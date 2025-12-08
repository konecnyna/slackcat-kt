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
        exposed()
        enableJunitTesting()
    }
}

val slackcatProperties = SlackcatProperties(project)

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.testing)
}
