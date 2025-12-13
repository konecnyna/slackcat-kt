package com.slackcat.publishing

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Validates that published POM files contain correct dependencies.
 * Run after: ./gradlew generatePomFileForGprPublication
 */
class PomValidationTest {

    @Test
    fun `slackcat POM should expose internal modules as dependencies`() {
        val pomFile = findPomFile("slackcat")
        val pomContent = pomFile.readText()

        // Should contain all internal module dependencies
        assertTrue(pomContent.contains("<artifactId>chat</artifactId>"), "Missing chat dependency")
        assertTrue(pomContent.contains("<artifactId>database</artifactId>"), "Missing database dependency")
        assertTrue(pomContent.contains("<artifactId>common</artifactId>"), "Missing common dependency")
        assertTrue(pomContent.contains("<artifactId>network</artifactId>"), "Missing network dependency")
        assertTrue(pomContent.contains("<artifactId>server</artifactId>"), "Missing server dependency")

        // All should be com.slackcat group
        assertTrue(pomContent.contains("<groupId>com.slackcat</groupId>"))
    }

    @Test
    fun `database POM should expose Exposed API dependencies`() {
        val pomFile = findPomFile("database")
        val pomContent = pomFile.readText()

        // Should expose Exposed dependencies as API (scope: compile)
        assertTrue(pomContent.contains("<artifactId>exposed-core</artifactId>"))
        assertTrue(pomContent.contains("<artifactId>exposed-dao</artifactId>"))
        assertTrue(pomContent.contains("<artifactId>exposed-jdbc</artifactId>"))

        // Check they're marked as compile scope (api dependencies)
        val exposedCoreDep = pomContent.substringAfter("<artifactId>exposed-core</artifactId>")
            .substringBefore("</dependency>")
        assertTrue(exposedCoreDep.contains("<scope>compile</scope>") || !exposedCoreDep.contains("<scope>"))
    }

    @Test
    fun `network POM should expose Ktor and serialization API`() {
        val pomFile = findPomFile("network")
        val pomContent = pomFile.readText()

        assertTrue(pomContent.contains("<artifactId>ktor-client-core-jvm</artifactId>"))
        assertTrue(pomContent.contains("<artifactId>kotlinx-serialization-json-jvm</artifactId>"))
    }

    private fun findPomFile(moduleName: String): File {
        val buildDir = File("build/publications/gpr")
        assertTrue(buildDir.exists(), "Build directory not found. Run: ./gradlew generatePomFileForGprPublication")

        val pomFile = buildDir.walkTopDown()
            .firstOrNull { it.name.startsWith("pom-") && it.extension == "xml" }
            ?: throw IllegalStateException("POM file not found in ${buildDir.absolutePath}")

        return pomFile
    }
}
