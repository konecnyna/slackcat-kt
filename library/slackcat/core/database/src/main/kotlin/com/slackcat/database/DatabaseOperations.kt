package com.slackcat.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
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
