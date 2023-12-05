package com.slackcat.plugins

import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension

plugins {
    id("com.autonomousapps.dependency-analysis")
    id("com.vanniktech.dependency.graph.generator")
}

configure<DependencyGraphGeneratorExtension> {
    generators.create("appGenerator") {
        include = { dependency -> dependency.moduleGroup.startsWith("app") }
    }
}
