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
    implementation(libs.commons.dbcp2)
    implementation(libs.dotenv.kotlin)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Implementation-Version"] = AppVersion.versionName
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("slackcat")
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Implementation-Version"] = AppVersion.versionName
    }
}

tasks.processResources {
    val versionProps = mapOf("version" to AppVersion.versionName)
    inputs.properties(versionProps)
    filesMatching("version.properties") {
        expand(versionProps)
    }
}

tasks.test {
    useJUnitPlatform()
}
