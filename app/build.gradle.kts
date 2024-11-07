import com.slackcat.SlackcatProperties
import com.slackcat.plugins.extentsion.SlackcatExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //id("com.slackcat.plugins.application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("plugin.serialization") version "1.5.21"
    application
    id("com.slackcat.plugins.base-internal")
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

application {
    mainClass.set("com.slackcat.app.MainKt")
}

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
    val manifestClasspath = configurations.runtimeClasspath.get().joinToString(" ") { it.name }
    manifest {
        attributes(
            "Implementation-Title" to "Test thing",
            "Implementation-Version" to "0.0.1",
            "Built-By" to System.getProperty("user.name"),
            "Built-JDK" to System.getProperty("java.version"),
            "Built-Gradle" to gradle.gradleVersion,
            "Class-Path" to manifestClasspath
        )
    }
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    manifest {
        attributes["Main-Class"] = "com.slackcat.app.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("slackcat")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest {
        attributes(mapOf(
            "Main-Class" to "com.slackcat.app.MainKt"
        ))
    }
}
