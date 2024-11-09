plugins {
    id("com.slackcat.plugins.root")
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

allprojects {
    group = "com.slackcat"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }

    // Apply the Kotlin plugin to all projects
    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        jvmToolchain(21)
    }
}