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
        koin()
        enableJunitTesting()
    }
}

val slackcatProperties = SlackcatProperties(project)

repositories {
    mavenCentral()
}

dependencies {
    api(projects.library.slackcat.data.chat)
    api(projects.library.slackcat.core.database)
    api(projects.library.slackcat.core.common)
    api(projects.library.slackcat.core.network)

    implementation("org.apache.commons:commons-dbcp2:2.13.0")
}
