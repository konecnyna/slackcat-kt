package com.slackcat.plugins

import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension
import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorTask
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style

plugins {
    id("com.autonomousapps.dependency-analysis")
    id("com.vanniktech.dependency.graph.generator")
}



configure<DependencyGraphGeneratorExtension> {
    generators.create("appGenerator") {
        include = { dependency -> dependency.moduleGroup.startsWith("app") }

    }

}