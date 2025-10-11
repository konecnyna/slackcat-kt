package com.slackcat.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

/**
 * Generic database query wrapper that runs operations in a suspended transaction.
 * Use this for all database operations to ensure proper async handling.
 */
suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

/**
 * Generic upsert operation that abstracts Exposed's upsert functionality.
 * Atomically inserts or updates a row based on unique keys.
 *
 * @param table The table to upsert into
 * @param keys Array of columns that define uniqueness (used for conflict detection)
 * @param onUpdate List of column updates to apply when key conflict occurs
 * @param insertBody Lambda to set values for insert case
 * @param selectWhere Condition to select the result row after upsert
 * @param mapper Function to map ResultRow to desired type
 * @return The upserted row mapped to type T
 */
suspend fun <T> dbUpsert(
    table: Table,
    keys: Array<Column<*>>,
    onUpdate: List<Pair<Column<*>, Expression<*>>>,
    insertBody: Table.(InsertStatement<*>) -> Unit,
    selectWhere: SqlExpressionBuilder.() -> Op<Boolean>,
    mapper: (ResultRow) -> T,
): T =
    dbQuery {
        table.upsert(
            keys = keys,
            onUpdate = onUpdate,
            body = insertBody,
        )

        table.select(selectWhere).single().let(mapper)
    }

/**
 * Selects rows from a table based on a where condition.
 */
fun Table.dbSelect(where: SqlExpressionBuilder.() -> Op<Boolean>): Query = this.select(where)

/**
 * Selects all rows from a table.
 */
fun Table.dbSelectAll(): Query = this.selectAll()

/**
 * Inserts a row into a table.
 * @return The InsertStatement result
 */
fun <T : Table> T.dbInsert(body: T.(InsertStatement<Number>) -> Unit): InsertStatement<Number> = this.insert(body)

/**
 * Deletes rows from a table based on a where condition.
 * @return The number of deleted rows
 */
inline fun <T : Table> T.dbDeleteWhere(crossinline op: SqlExpressionBuilder.() -> Op<Boolean>): Int =
    deleteWhere {
        SqlExpressionBuilder.op()
    }

/**
 * SQL expression builder for creating conditions.
 */
object DbOp {
    /**
     * Equality operator.
     */
    infix fun <T> Column<T>.eq(value: T): Op<Boolean> =
        org.jetbrains.exposed.sql.SqlExpressionBuilder.run { this@eq eq value }

    /**
     * Greater than operator.
     */
    infix fun <T : Comparable<T>> Column<T>.greater(value: T): Op<Boolean> =
        org.jetbrains.exposed.sql.SqlExpressionBuilder.run { this@greater greater value }

    /**
     * And operator for combining conditions.
     */
    infix fun Op<Boolean>.and(other: Op<Boolean>): Op<Boolean> = this and other

    /**
     * Addition operator for numeric columns.
     */
    operator fun <T> Column<T>.plus(value: Int): Expression<T> =
        org.jetbrains.exposed.sql.SqlExpressionBuilder.run { this@plus + value }
}

/**
 * Sort order enum for query ordering.
 */
enum class DbSortOrder {
    ASC,
    DESC,
    ;

    fun toExposed(): SortOrder =
        when (this) {
            ASC -> SortOrder.ASC
            DESC -> SortOrder.DESC
        }
}

/**
 * Extension function to order query results.
 */
fun Query.dbOrderBy(
    column: Expression<*>,
    order: DbSortOrder = DbSortOrder.ASC,
): Query = this.orderBy(column, order.toExposed())

/**
 * Extension function to limit query results.
 */
fun Query.dbLimit(n: Int): Query = this.limit(n)

// Test helper functions

/**
 * Creates a test database connection (for testing purposes).
 * @param jdbcUrl The JDBC URL for the database connection
 * @param driver The JDBC driver class name
 * @return The database instance
 */
fun createTestDatabase(
    jdbcUrl: String,
    driver: String,
): org.jetbrains.exposed.sql.Database = org.jetbrains.exposed.sql.Database.connect(jdbcUrl, driver = driver)

/**
 * Creates database schema for the given tables (for testing purposes).
 * @param database The database instance
 * @param tables The tables to create
 */
fun createTestSchema(
    database: org.jetbrains.exposed.sql.Database,
    vararg tables: Table,
) {
    org.jetbrains.exposed.sql.transactions.transaction(database) {
        org.jetbrains.exposed.sql.SchemaUtils.create(*tables)
    }
}

/**
 * Drops database schema for the given tables (for testing purposes).
 * @param database The database instance
 * @param tables The tables to drop
 */
fun dropTestSchema(
    database: org.jetbrains.exposed.sql.Database,
    vararg tables: Table,
) {
    org.jetbrains.exposed.sql.transactions.transaction(database) {
        org.jetbrains.exposed.sql.SchemaUtils.drop(*tables)
    }
}
