import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.slackcat.plugins.root")
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.5.21"
    application
}

allprojects {
    group = "org.defkon"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}