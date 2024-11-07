
plugins {
    id("com.slackcat.plugins.root")
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false

}

allprojects {
    group = "com.slackcat"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}
