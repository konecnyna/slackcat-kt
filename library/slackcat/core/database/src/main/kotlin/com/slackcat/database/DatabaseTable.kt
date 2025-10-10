package com.slackcat.database

import org.jetbrains.exposed.sql.Table

/**
 * Marks APIs that directly expose the underlying Exposed SQL implementation.
 *
 * **Warning:** This API is unstable and may change at any time. Using this annotation
 * means you're opting into implementation details that may break in future versions.
 *
 * Only use this if you need direct access to Exposed SQL features that aren't
 * available through the DatabaseTable abstraction.
 *
 * Example usage:
 * ```kotlin
 * @OptIn(UnstableExposedApi::class)
 * fun doAdvancedQuery() {
 *     val exposedTable = myTable.toExposedTable()
 *     // Use Exposed SQL directly
 * }
 * ```
 */
@RequiresOptIn(
    message = "This API exposes the underlying Exposed SQL implementation and may change at any time. " +
        "Use only if you need features not available through DatabaseTable abstraction.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class UnstableExposedApi

/**
 * Wrapper interface for database tables to hide Exposed SQL implementation from modules.
 * Modules should use this interface instead of directly depending on Exposed's Table class.
 */
interface DatabaseTable {
    /**
     * Returns the underlying Exposed Table for advanced use cases.
     *
     * **Warning:** This method is marked with [@UnstableExposedApi] and may change at any time.
     * You must explicitly opt-in to use this method with `@OptIn(UnstableExposedApi::class)`.
     *
     * This is intended for advanced users who need direct access to Exposed SQL features
     * that aren't available through the DatabaseTable abstraction. For most use cases,
     * you should use the DatabaseTable interface methods instead.
     *
     * @return The underlying Exposed Table instance
     */
    @UnstableExposedApi
    fun toExposedTable(): Table
}

/**
 * Extension function to wrap an Exposed Table as a DatabaseTable.
 */
fun Table.asDatabaseTable(): DatabaseTable =
    object : DatabaseTable {
        override fun toExposedTable(): Table = this@asDatabaseTable
    }
