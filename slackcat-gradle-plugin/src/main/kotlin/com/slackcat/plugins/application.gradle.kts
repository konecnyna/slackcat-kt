package com.slackcat.plugins

import com.slackcat.plugins.extentsion.defaultKotlinCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.slackcat.plugins.base-internal")
    kotlin("jvm")
}
//
//application {
//    mainClass.set("com.slackcat.app.MainKt")
//}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("17"))
        suppressWarnings.set(true)
        freeCompilerArgs.set(defaultKotlinCompilerArguments)
    }
}
