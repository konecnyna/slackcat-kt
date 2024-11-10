package com.slackcat.plugins

plugins {
    application
    id("com.slackcat.plugins.base-internal")
    kotlin("jvm")
}

application { mainClass.set("com.slackcat.app.ApplicationKt") }
