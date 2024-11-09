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
