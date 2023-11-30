import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
}

if (JavaVersion.current().majorVersion != "17") {
    throw GradleException(
        """
        The Java version used ${JavaVersion.current()} is not the expected version 17.
        """.trimIndent(),
    )
}
