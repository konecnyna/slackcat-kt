package com.slackcat.plugins

import com.slackcat.SlackcatProperties

private val slackcatProperties = SlackcatProperties(project)
val patterns: List<String> = (listOf("**/src/**/*.kt", "**/src/**/*.kts", "!*/build/*")).sortedDescending()
val ktlint: Configuration by configurations.creating
val ktlintConfig: Configuration by configurations.creating

dependencies {
    // https://pinterest.github.io/ktlint/latest/install/integrations/
    ktlint("com.pinterest.ktlint:ktlint-cli:1.0.1") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val fileName = "ktlint-rules.editorconfig"
val configDir: Provider<RegularFile> = layout.buildDirectory.file("ktlint/")
val configFile: Provider<RegularFile> = layout.buildDirectory.file("ktlint/$fileName")

// To use when preparing updates to the editorconfig
val localConfigFile: RegularFile = layout.projectDirectory.file("static-analysis/ktlint/.editorconfig")

tasks.register<Copy>("copyKtlintConfig") {
    from(ktlintConfig) {
        include("*.editorconfig")
    }
    into(configDir)
    rename("(.+)", fileName)
}

tasks.register("ktlint", JavaExec::class) {
    dependsOn("copyKtlintConfig")
    description = "Check Kotlin code style."
    classpath = ktlint
    jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf(
        "--reporter=plain",
        "--reporter=checkstyle,output=${layout.buildDirectory.asFile.get().absolutePath}/reports/ktlint/ktlint.xml",
        "--color",
        "--editorconfig=${configFile.get().asFile.absolutePath}",

    ) + patterns
}

tasks.register("ktlintFormat", JavaExec::class) {
    dependsOn("copyKtlintConfig")
    val arguments = listOf("-F", "--editorconfig=${configFile.get().asFile.absolutePath}") + patterns
    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
    mainClass.set("com.pinterest.ktlint.Main")
    args = arguments
}
