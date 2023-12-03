package features.slackcat.models

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table


data class TableEntity(
    val name: String,
    val columns: List<Columns>
)


data class Columns(
    val name : String,
    val type: ColumnType,
    val isPrimaryKey: Boolean = false
)


enum class ColumnType {
    Int,
    Text,
}




fun TableEntity.createExposedTable(): Table {
    return object : Table(name = this@createExposedTable.name) {
        init {
            this@createExposedTable.columns.forEach { column ->
                val exposedColumn = column.type.toExposedType(this, column.name)
                if (column.isPrimaryKey) {
                    //primaryKey
                }
            }
        }
    }
}




fun ColumnType.toExposedType(table: Table, columnName: String): Column<*> {
    return when (this) {
        ColumnType.Int -> table.integer(columnName)
        ColumnType.Text -> table.text(columnName)
    }
}