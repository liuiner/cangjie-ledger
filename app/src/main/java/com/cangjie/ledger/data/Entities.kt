package com.cangjie.ledger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object EntryType {
    const val EXPENSE = "expense"
    const val INCOME = "income"
}

@Entity(tableName = "books", indices = [Index(value = ["name"], unique = true)])
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long = 0xFF0E6B58,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type", "name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val emoji: String,
    val color: Long = 0xFF8AA39B,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
)

@Entity(
    tableName = "records",
    indices = [
        Index(value = ["bookId", "dateEpochDay"]),
        Index(value = ["categoryId"]),
        Index(value = ["recurringRuleId"]),
    ],
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val categoryId: Long,
    val type: String,
    val amountCents: Long,
    val dateEpochDay: Long,
    val note: String = "",
    val recurringRuleId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "record_tags",
    primaryKeys = ["recordId", "tagId"],
    indices = [Index(value = ["tagId"])],
)
data class RecordTagCrossRef(
    val recordId: Long,
    val tagId: Long,
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["bookId", "categoryId", "monthKey"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val categoryId: Long,
    val monthKey: String,
    val limitCents: Long,
)

@Entity(
    tableName = "recurring_rules",
    indices = [Index(value = ["bookId"]), Index(value = ["categoryId"])],
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val categoryId: Long,
    val type: String,
    val amountCents: Long,
    val dayOfMonth: Int,
    val note: String = "",
    val enabled: Boolean = true,
    val createdEpochDay: Long,
)

@Entity(
    tableName = "recurring_occurrences",
    indices = [Index(value = ["ruleId", "periodKey"], unique = true)],
)
data class RecurringOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val periodKey: String,
    val recordId: Long,
    val generatedAt: Long = System.currentTimeMillis(),
)

