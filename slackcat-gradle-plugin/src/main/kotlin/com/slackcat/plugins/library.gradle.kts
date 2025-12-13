package com.slackcat.plugins

import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("com.slackcat.plugins.base-internal")
    `maven-publish`
    `java-library`
}

configure<JavaPluginExtension> {
    withSourcesJar()
}

// Bundle internal module classes into the main JAR
tasks.named<Jar>("jar") {
    // Include classes from project dependencies (internal modules)
    val projectDeps =
        configurations.runtimeClasspath.get().allDependencies
            .filterIsInstance<org.gradle.api.artifacts.ProjectDependency>()
            .filter { it.dependencyProject.group == "com.slackcat" }
            .map { it.dependencyProject }

    projectDeps.forEach { dep ->
        val jarTask = dep.tasks.named<Jar>("jar").get()
        dependsOn(jarTask)
        from(zipTree(jarTask.archiveFile))
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.token") as String?
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            // Exclude internal modules from POM dependencies - they're bundled in the JAR
            pom.withXml {
                val dependenciesNode = asNode().get("dependencies")
                if (dependenciesNode is groovy.util.NodeList && dependenciesNode.isNotEmpty()) {
                    val depsNode = dependenciesNode[0] as groovy.util.Node
                    val toRemove = mutableListOf<groovy.util.Node>()

                    // Collect dependencies to remove (avoid concurrent modification)
                    depsNode.children().forEach { child ->
                        if (child is groovy.util.Node) {
                            val groupIdNode = child.get("groupId")
                            val artifactIdNode = child.get("artifactId")

                            if (groupIdNode is groovy.util.NodeList &&
                                artifactIdNode is groovy.util.NodeList &&
                                groupIdNode.isNotEmpty() &&
                                artifactIdNode.isNotEmpty()) {

                                val groupId = groupIdNode[0].toString()
                                val artifactId = artifactIdNode[0].toString()

                                // Remove internal slackcat modules (they're bundled in the JAR)
                                // Keep only slackcat and slackcat-modules as valid transitive dependencies
                                if (groupId == "com.slackcat" && artifactId !in listOf("slackcat", "slackcat-modules")) {
                                    toRemove.add(child)
                                }
                            }
                        }
                    }

                    // Remove all marked dependencies
                    toRemove.forEach { depsNode.remove(it) }
                }
            }

            pom {
                name.set(project.name)
                description.set("Slackcat library module")
                url.set("https://github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("slackcat")
                        name.set("Slackcat Team")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}.git")
                    developerConnection.set(
                        "scm:git:ssh://github.com:${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}.git",
                    )
                    url.set("https://github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}")
                }
            }
        }
    }
}
