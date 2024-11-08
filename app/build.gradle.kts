import com.slackcat.SlackcatProperties

plugins {
    id("com.slackcat.plugins.application")
    id("com.github.johnrengelman.shadow")
    kotlin("plugin.serialization")
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
    implementation(projects.library.slackcat)
    implementation(projects.library.data.chat)

    implementation(libs.serialization.json)
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
