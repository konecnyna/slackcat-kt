import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.slackcat.plugins.base-internal")
    id("com.github.johnrengelman.shadow")
    kotlin("plugin.serialization") version "1.5.21"
}


application {
    mainClass.set("com.slackcat.app.ApplicationKt")
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
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:server"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
}


tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Implementation-Version"] = "0.0.1" // Change to project version
    }
}



tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("slackcat")
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Implementation-Version"] = "0.0.1" // Change to project version
    }
}
