import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.defaultKotlinCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

}

