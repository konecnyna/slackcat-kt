import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.slackcat.plugins.application")
    kotlin("plugin.serialization") version "1.5.21"
}

repositories {
    mavenCentral()
}


slackcat {
    features {
        coroutines()
        reflection()
        exposed()
    }
}

val slackcatProperties = SlackcatProperties(project)


repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":features:slackcat"))
    implementation(project(":data:chat"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
}