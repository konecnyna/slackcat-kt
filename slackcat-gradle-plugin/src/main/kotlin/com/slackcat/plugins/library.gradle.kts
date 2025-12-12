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
                    developerConnection.set("scm:git:ssh://github.com:${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}.git")
                    url.set("https://github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}")
                }
            }
        }
    }
}
