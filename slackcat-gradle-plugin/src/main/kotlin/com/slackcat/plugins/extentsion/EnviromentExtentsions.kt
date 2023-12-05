package com.slackcat.plugins.extentsion

val isCiEnvironment: Boolean
    get() = System.getenv("CI")?.toBoolean() == true

val defaultMaxParallelForks: Int
    get() = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1

val defaultKotlinCompilerArguments: List<String>
    get() =
        listOf(
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
