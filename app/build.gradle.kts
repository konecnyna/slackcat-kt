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
        koin()
    }
}

val slackcatProperties = SlackcatProperties(project)

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.library.slackcat)
    implementation(projects.library.slackcatModules)

    implementation(libs.serialization.json)
    implementation("org.apache.commons:commons-dbcp2:2.13.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
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


tasks.test {
    useJUnitPlatform()
}

