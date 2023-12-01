package com.slackcat.plugins

import com.slackcat.plugins.extentsion.defaultKotlinCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.slackcat.plugins.base-internal")
    kotlin("jvm")
}
