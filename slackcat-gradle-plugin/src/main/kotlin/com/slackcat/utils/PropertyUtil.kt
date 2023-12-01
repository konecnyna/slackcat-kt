@file:JvmName("PropertyUtil")

package com.slackcat.utils

// https://github.com/slackhq/slack-gradle-plugin/blob/72f06d88f7a800d3902ce0dfe4de96af036148f2/slack-plugin/src/main/kotlin/slack/gradle/util/PropertyUtil.kt
import org.gradle.api.Project

/** Gets or creates a cached extra property. */
internal fun <T> Project.getOrCreateExtra(
    key: String,
    body: (Project) -> T,
): T {
    with(project.extensions.extraProperties) {
        if (!has(key)) {
            set(key, body(project))
        }
        @Suppress("UNCHECKED_CAST")
        return get(key) as? T ?: body(project) // Fallback if multiple class loaders are involved
    }
}
