package com.slackcat.plugins

import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension
import com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorPlugin
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style

plugins {
    id("com.autonomousapps.dependency-analysis")
    id("com.vanniktech.dependency.graph.generator")
}


configure<DependencyGraphGeneratorExtension> {
    generators.create("jetbrainsLibraries") {
        include = { dependency -> dependency.moduleGroup.startsWith("org.jetbrains") } // Only want Jetbrains.
        children = { true } // Include transitive dependencies.
        dependencyNode = { node, dependency -> node.add(Style.FILLED, Color.rgb("#AF1DF5")) } // Give them some color.
    }
}