import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

plugins {
    `kotlin-dsl`
}


dependencies {
//    implementation(libs.gradlePluginKotlin)
}

//if (JavaVersion.current().majorVersion != "17") {
//    throw GradleException(
//        """
//        The Java version used ${JavaVersion.current()} is not the expected version 17.
//
//        More details here:
//        https://stashinvest.github.io/stash-invest-android/getting-started/java/
//        """.trimIndent(),
//    )
//}
